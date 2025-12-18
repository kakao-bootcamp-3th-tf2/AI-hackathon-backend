package jojo.jjdc.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.googlecalendar.ai.AiServerResponse.AiSuggestion;
import jojo.jjdc.googlecalendar.ai.GoogleCalendarAiService;
import jojo.jjdc.googlecalendar.ai.GoogleCalendarAiResponse;
import jojo.jjdc.googlecalendar.client.GoogleCalendarClient;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventDto;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarAppendRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarSuggestResponse;
import jojo.jjdc.googlecalendar.dto.SuggestDto;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.security.oauth.entity.GoogleOAuthToken;
import jojo.jjdc.security.oauth.service.GoogleOAuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final String JJDC_CALENDAR_SUMMARY = "JJDC";
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    private final GoogleOAuthTokenService googleOAuthTokenService;
    private final GoogleCalendarAiService googleCalendarAiService;
    private final GoogleCalendarClient googleCalendarClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * 인증된 사용자의 JJDC 캘린더에서 이벤트 목록을 조회한다.
     */
    public List<GoogleCalendarEventDto> getEvents(MemberPrincipal principal, OffsetDateTime from, OffsetDateTime to) {
        Long memberId = resolveMemberId(principal);
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        GoogleCalendarClient.GoogleEventsResponse response = googleCalendarClient.fetchEvents(accessToken, calendarId, from.toInstant(), to.toInstant());
        return toDtoList(response);
    }

    /**
     * 카테고리/브랜드/기간 정보를 받아 AI 보조 설명을 포함한 캘린더 일정을 등록한다.
     */
    public GoogleCalendarEventDto createEvent(MemberPrincipal principal, GoogleCalendarCreateEventRequest request) {
        Long memberId = resolveMemberId(principal);
        GoogleCalendarAiResponse aiResponse = googleCalendarAiService.requestAiResult(request, principal);
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        GoogleCalendarClient.GoogleCalendarEventInsertRequest insertRequest = buildEventInsertRequest(request, aiResponse);
        GoogleCalendarClient.GoogleCalendarCreatedEventResponse created = googleCalendarClient.insertEvent(accessToken, calendarId, insertRequest);
        return toDtoFromCreated(created, toAiSuggestions(aiResponse));
    }

    /**
     * 여러 일정 ID를 받아 AI 설명을 붙이고, 각 결과를 반환한다.
     */
    public List<GoogleCalendarSuggestResponse> appendSuggest(MemberPrincipal principal, List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        Long memberId = resolveMemberId(principal);
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        List<GoogleCalendarSuggestResponse> responses = new ArrayList<>();
        for (String eventId : eventIds) {
            responses.add(processAppend(accessToken, calendarId, eventId, principal));
        }
        return responses;
    }

    /**
     * 단일 일정에 대해 AI 설명을 붙이고, 결과를 반환한다.
     */
    private GoogleCalendarSuggestResponse processAppend(String accessToken, String calendarId, String eventId, MemberPrincipal principal) {
        GoogleCalendarClient.GoogleCalendarEventDetailResponse detail;
        try {
            detail = googleCalendarClient.fetchEventDetail(accessToken, calendarId, eventId);
        } catch (BusinessException ex) {
            return new GoogleCalendarSuggestResponse(eventId, "");
        }
        Optional<GoogleCalendarAppendRequest> parsed = parseSummary(detail.summary());
        if (parsed.isEmpty()) {
            return new GoogleCalendarSuggestResponse(eventId, "");
        }
        GoogleCalendarAppendRequest appendRequest = parsed.get();
        OffsetDateTime start = toOffset(detail.start());
        OffsetDateTime end = toOffset(detail.end());
        GoogleCalendarAiResponse aiResponse = googleCalendarAiService.requestAppendAiResult(
                eventId,
                detail.summary(),
                start,
                end,
                appendRequest,
                principal
        );
        String mergedDescription = mergeDescriptions(detail.description(), aiResponse.benefitDescription());
        GoogleCalendarClient.GoogleCalendarCreatedEventResponse patched =
                googleCalendarClient.patchEvent(accessToken, calendarId, eventId, mergedDescription);
        List<SuggestDto> suggestions = appendRequest.needSuggestList() ? toAiSuggestions(aiResponse) : List.of();
        return new GoogleCalendarSuggestResponse(eventId, toDtoFromCreated(patched, suggestions));
    }

    /**
     * 일정 제목에서 카테고리/브랜드를 추출하여 AI 요청 정보로 변환한다.
     */
    private Optional<GoogleCalendarAppendRequest> parseSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return Optional.empty();
        }
        String[] tokens = summary.split("#");
        if (tokens.length < 3) {
            return Optional.empty();
        }
        String category = tokens[1].trim();
        String brand = tokens[2].trim();
        if (category.isEmpty() || brand.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GoogleCalendarAppendRequest(category, brand, true));
    }

    private List<GoogleCalendarEventDto> toDtoList(GoogleCalendarClient.GoogleEventsResponse response) {
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .map(item -> toDto(item, fallbackStart()))
                .collect(Collectors.toList());
    }

    private GoogleCalendarEventDto toDto(GoogleCalendarClient.GoogleEventItem item, Instant fallback) {
        Instant start = item.start() != null ? item.start().instant() : fallback;
        Instant end = item.end() != null ? item.end().instant() : start;
        return new GoogleCalendarEventDto(
                item.id(),
                item.summary(),
                start,
                end,
                List.of(),
                item.description()
        );
    }

    /**
     * MemberPrincipal에서 memberId를 추출하며 누락 시 인증 예외를 발생시킨다.
     */
    private Long resolveMemberId(MemberPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.memberId();
    }

    private String ensureJjdcCalendarId(String accessToken) {
        return googleCalendarClient.findCalendarId(accessToken, JJDC_CALENDAR_SUMMARY)
                .orElseGet(() -> googleCalendarClient.createCalendar(accessToken, JJDC_CALENDAR_SUMMARY, DEFAULT_TIMEZONE.getId()));
    }

    private String ensureAccessToken(GoogleOAuthToken token) {
        Instant expiresAt = token.getAccessTokenExpiredAt();
        if (expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return token.getAccessToken();
        }
        return refreshAccessToken(token);
    }

    private String refreshAccessToken(GoogleOAuthToken token) {
        if (token.getRefreshToken() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
        GoogleCalendarClient.GoogleTokenRefreshResponse payload =
                googleCalendarClient.refreshAccessToken(clientId, clientSecret, token.getRefreshToken());
        Long expiresIn = payload.expiresIn();
        if (expiresIn == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "token 재발급 응답에 expiresIn 누락");
        }
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);
        googleOAuthTokenService.updateAccessToken(token, payload.accessToken(), expiresAt);
        return payload.accessToken();
    }

    private GoogleCalendarClient.GoogleCalendarEventInsertRequest buildEventInsertRequest(
            GoogleCalendarCreateEventRequest request,
            GoogleCalendarAiResponse aiResponse
    ) {
        OffsetDateTime start = aiResponse.startAt() != null ? aiResponse.startAt() : request.startAt();
        OffsetDateTime end = aiResponse.endAt() != null ? aiResponse.endAt() : request.endAt();
        String summary = buildSummary(request);
        String description = describeAiBenefits(request, aiResponse);
        return new GoogleCalendarClient.GoogleCalendarEventInsertRequest(
                summary,
                description,
                new GoogleCalendarClient.GoogleEventDateTime(start),
                new GoogleCalendarClient.GoogleEventDateTime(end)
        );
    }

    private String buildSummary(GoogleCalendarCreateEventRequest request) {
        return "#" + request.category() + "#" + request.brand();
    }

    private String describeAiBenefits(GoogleCalendarCreateEventRequest request, GoogleCalendarAiResponse aiResponse) {
        StringBuilder builder = new StringBuilder();
        builder.append("카테고리: ").append(request.category()).append("\n");
        builder.append("브랜드: ").append(request.brand()).append("\n");
        builder.append("기간: ").append(formatRange(aiResponse)).append("\n");
        builder.append("AI 혜택 요약: ").append(aiResponse.benefitDescription());
        return builder.toString();
    }

    private String formatRange(GoogleCalendarAiResponse aiResponse) {
        OffsetDateTime start = aiResponse.startAt();
        OffsetDateTime end = aiResponse.endAt();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        if (start != null && end != null) {
            return start.format(formatter) + " ~ " + end.format(formatter);
        }
        return "기간 정보 없음";
    }

    private GoogleCalendarEventDto toDtoFromCreated(
            GoogleCalendarClient.GoogleCalendarCreatedEventResponse response,
            List<SuggestDto> suggestList
    ) {
        Instant start = response.start() != null ? response.start().instant() : fallbackStart();
        Instant end = response.end() != null ? response.end().instant() : start;
        return new GoogleCalendarEventDto(
                response.id(),
                response.summary(),
                start,
                end,
                suggestList != null ? suggestList : List.of(),
                response.description()
        );
    }

    /**
     * GoogleCalendarClient.GoogleEventTime을 OffsetDateTime으로 변환한다.
     */
    private OffsetDateTime toOffset(GoogleCalendarClient.GoogleEventTime time) {
        Instant instant = time != null ? time.instant() : Instant.now();
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String mergeDescriptions(String existing, String aiResult) {
        if (existing == null || existing.isBlank()) {
            return aiResult;
        }
        return existing + "\n\n" + aiResult;
    }

    private Instant fallbackStart() {
        return Instant.now();
    }

    private List<SuggestDto> toAiSuggestions(GoogleCalendarAiResponse aiResponse) {
        if (aiResponse == null || aiResponse.suggestions() == null) {
            return List.of();
        }
        return aiResponse.suggestions().stream()
                .map(this::toAiSuggestion)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SuggestDto toAiSuggestion(AiSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }
        return new SuggestDto(
                suggestion.suggest(),
                parseOffset(suggestion.fromDate()),
                parseOffset(suggestion.toDate())
        );
    }

    private OffsetDateTime parseOffset(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
