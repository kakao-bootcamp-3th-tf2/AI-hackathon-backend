package jojo.jjdc.service.googlecalendar;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
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
import jojo.jjdc.dto.AiRecommendationItem;
import jojo.jjdc.service.NotityService;
import jojo.jjdc.service.ai.GoogleCalendarAiService;
import jojo.jjdc.service.ai.GoogleCalendarAiResponse;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarCreateEventRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarEventDto;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarAppendRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarManualUpdateRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarSuggestResponse;
import jojo.jjdc.dto.googlecalendar.SuggestDto;
import jojo.jjdc.dto.NotityDto;
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
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String KST_ID = "Asia/Seoul";

    private final GoogleOAuthTokenService googleOAuthTokenService;
    private final GoogleCalendarAiService googleCalendarAiService;
    private final GoogleCalendarClient googleCalendarClient;
    private final NotityService notityService;

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
     * 카테고리/브랜드/기간 정보를 받아 AI 보조 설명을 포함한 캘린더 일정을 등록하고, 알람 정보를 저장한다.
     */
    public NotityDto createEvent(MemberPrincipal principal, GoogleCalendarCreateEventRequest request) {
        Long memberId = resolveMemberId(principal);
        GoogleCalendarAiResponse aiResponse = googleCalendarAiService.requestAiResult(request, principal);
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        GoogleCalendarClient.GoogleCalendarEventInsertRequest insertRequest = buildEventInsertRequest(request, aiResponse);
        GoogleCalendarClient.GoogleCalendarCreatedEventResponse created = googleCalendarClient.insertEvent(accessToken, calendarId, insertRequest);
        GoogleCalendarEventDto dto = toDtoFromCreated(created, toAiSuggestions(aiResponse));
        return registerNotity(memberId, dto);
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
            responses.add(processAppend(memberId, accessToken, calendarId, eventId, principal));
        }
        return responses;
    }

    /**
     * 단일 일정에 대해 AI 설명을 붙이고, 결과를 반환한다.
     */
    private GoogleCalendarSuggestResponse processAppend(Long memberId, String accessToken, String calendarId, String eventId, MemberPrincipal principal) {
        GoogleCalendarClient.GoogleCalendarEventDetailResponse detail;
        try {
            detail = googleCalendarClient.fetchEventDetail(accessToken, calendarId, eventId);
        } catch (BusinessException ex) {
            return new GoogleCalendarSuggestResponse(eventId, null);
        }
        Optional<GoogleCalendarAppendRequest> parsed = parseSummary(detail.summary());
        if (parsed.isEmpty()) {
            return new GoogleCalendarSuggestResponse(eventId, null);
        }
        GoogleCalendarAppendRequest appendRequest = parsed.get();
        OffsetDateTime start = toKst(toOffset(detail.start()));
        OffsetDateTime end = toKst(toOffset(detail.end()));
        GoogleCalendarAiResponse aiResponse = googleCalendarAiService.requestAppendAiResult(
                eventId,
                detail.summary(),
                start,
                end,
                appendRequest,
                principal
        );
        String overwrittenDescription = aiResponse.benefitDescription();
        GoogleCalendarClient.GoogleCalendarEventPatchRequest patchRequest =
                new GoogleCalendarClient.GoogleCalendarEventPatchRequest(
                        detail.summary(),
                        overwrittenDescription,
                        new GoogleCalendarClient.GoogleEventDateTime(start.toInstant(), "UTC", null),
                        new GoogleCalendarClient.GoogleEventDateTime(end.toInstant(), "UTC", null)
                );
        GoogleCalendarClient.GoogleCalendarCreatedEventResponse patched =
                googleCalendarClient.patchEvent(accessToken, calendarId, eventId, patchRequest);
        List<SuggestDto> suggestions = appendRequest.needSuggestList() ? toAiSuggestions(aiResponse) : List.of();
        GoogleCalendarEventDto eventDto = toDtoFromCreated(patched, suggestions);
        NotityDto notity = registerNotity(memberId, eventDto);
        return new GoogleCalendarSuggestResponse(eventId, notity);
    }

    /**
     * 프론트에서 전달한 일정/혜택 정보를 그대로 덮어써서 구글 캘린더와 알람을 갱신한다.
     */
    public NotityDto overwriteEvent(MemberPrincipal principal, GoogleCalendarManualUpdateRequest request) {
        Long memberId = resolveMemberId(principal);
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "eventId는 필수입니다.");
        }
        if (request.startAt() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "startAt은 필수입니다.");
        }
        OffsetDateTime start = toKst(request.startAt());
        OffsetDateTime end = ensureEndAfterStart(start, null);
        List<SuggestDto> suggestions = describeSuggestAsSingle(request.suggest());
        String description = describeManualContent(request.suggest(), suggestions);

        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        GoogleCalendarClient.GoogleCalendarEventDetailResponse original =
                googleCalendarClient.fetchEventDetail(accessToken, calendarId, request.eventId());

        GoogleCalendarClient.GoogleCalendarEventPatchRequest patchRequest =
                new GoogleCalendarClient.GoogleCalendarEventPatchRequest(
                        original.summary(),
                        description,
                        new GoogleCalendarClient.GoogleEventDateTime(start.toInstant(), "UTC", null),
                        new GoogleCalendarClient.GoogleEventDateTime(end.toInstant(), "UTC", null)
                );
        GoogleCalendarClient.GoogleCalendarCreatedEventResponse patched =
                googleCalendarClient.patchEvent(accessToken, calendarId, request.eventId(), patchRequest);

        GoogleCalendarEventDto eventDto = toDtoFromCreated(patched, suggestions);
        return registerNotity(memberId, eventDto);
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

    private NotityDto registerNotity(Long memberId, GoogleCalendarEventDto eventDto) {
        return notityService.upsert(
                memberId,
                eventDto.id(),
                eventDto.startAt(),
                eventDto.endAt(),
                eventDto.summary(),
                eventDto.content(),
                eventDto.suggestList()
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
        OffsetDateTime startKst = toKst(start);
        OffsetDateTime endKst = toKst(end);
        String summary = buildSummary(request);
        String description = describeAiBenefits(request, aiResponse);
        return new GoogleCalendarClient.GoogleCalendarEventInsertRequest(
                summary,
                description,
                new GoogleCalendarClient.GoogleEventDateTime(startKst.toInstant(), KST_ID, null),
                new GoogleCalendarClient.GoogleEventDateTime(endKst.toInstant(), KST_ID, null)
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

    private String describeManualContent(String content, List<SuggestDto> suggestions) {
        StringBuilder builder = new StringBuilder();
        if (content != null && !content.isBlank()) {
            builder.append(content.trim());
        }
        if (!suggestions.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("혜택 제안:\n");
            for (int i = 0; i < suggestions.size(); i++) {
                SuggestDto suggestion = suggestions.get(i);
                builder.append(i + 1).append(". ").append(suggestion.suggest());
                if (suggestion.startAt() != null || suggestion.endAt() != null) {
                    builder.append(" (")
                            .append(suggestion.startAt() != null ? suggestion.startAt() : "-")
                            .append(" ~ ")
                            .append(suggestion.endAt() != null ? suggestion.endAt() : "-")
                            .append(")");
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private Instant fallbackStart() {
        return Instant.now();
    }

    private OffsetDateTime toKst(OffsetDateTime dateTime) {
        ZoneOffset kstOffset = ZoneOffset.ofHours(9);
        if (dateTime == null) {
            return OffsetDateTime.now(KST).withOffsetSameLocal(kstOffset);
        }
        return dateTime.withOffsetSameLocal(kstOffset);
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

    private SuggestDto toAiSuggestion(AiRecommendationItem item) {
        if (item == null) {
            return null;
        }
        return new SuggestDto(
                item.message(),
                parseOffset(item.startAt()),
                parseOffset(item.endAt())
        );
    }

    private List<SuggestDto> describeSuggestAsSingle(String suggest) {
        if (suggest == null || suggest.isBlank()) {
            return List.of();
        }
        return List.of(new SuggestDto(suggest.trim(), null, null));
    }

    private OffsetDateTime ensureEndAfterStart(OffsetDateTime start, OffsetDateTime requestedEnd) {
        OffsetDateTime end = requestedEnd != null ? requestedEnd : start.plusMinutes(1);
        if (!end.isAfter(start)) {
            return start.plusMinutes(1);
        }
        return end;
    }

    private OffsetDateTime parseOffset(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            try {
                LocalDate date = LocalDate.parse(text);
                return date.atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
