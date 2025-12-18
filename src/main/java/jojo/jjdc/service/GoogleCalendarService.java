package jojo.jjdc.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.googlecalendar.ai.GoogleCalendarAiResponse;
import jojo.jjdc.googlecalendar.ai.GoogleCalendarAiService;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventDto;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventsResponse;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.security.oauth.entity.GoogleOAuthToken;
import jojo.jjdc.security.oauth.service.GoogleOAuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final String CALENDAR_BASE_URL = "https://www.googleapis.com/calendar/v3";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String JJDC_CALENDAR_SUMMARY = "JJDC";
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    private final GoogleOAuthTokenService googleOAuthTokenService;
    private final RestTemplate restTemplate;
    private final GoogleCalendarAiService googleCalendarAiService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * 인증된 사용자의 JJDC 캘린더에서 이벤트 목록을 조회한다.
     */
    public GoogleCalendarEventsResponse fetchPrimaryEvents(MemberPrincipal principal, OffsetDateTime from, OffsetDateTime to) {
        Long memberId = resolveMemberId(principal);
        Instant fromInstant = from.toInstant();
        Instant toInstant = to.toInstant();
        List<GoogleCalendarEventDto> events = collectMemberEvents(memberId, fromInstant, toInstant);
        return new GoogleCalendarEventsResponse(events);
    }

    /**
     * 카테고리/브랜드/기간 정보를 받아 AI 보조 설명을 포함한 캘린더 일정을 등록한다.
     */
    public GoogleCalendarEventDto createCategorizedEvent(MemberPrincipal principal, GoogleCalendarCreateEventRequest request) {
        Long memberId = resolveMemberId(principal);
        GoogleCalendarAiResponse aiResponse = googleCalendarAiService.requestAiResult(request, principal);
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        GoogleCalendarEventInsertRequest insertRequest = buildEventInsertRequest(request, aiResponse);
        return insertEvent(accessToken, calendarId, insertRequest);
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

    /**
     * 지정된 기간 동안 사용자의 JJDC 캘린더 이벤트 목록을 외부 API로부터 가져온다.
     */
    private List<GoogleCalendarEventDto> collectMemberEvents(Long memberId, Instant from, Instant to) {
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);
        ResponseEntity<GoogleEventsResponse> response = fetchEventsFromApi(accessToken, calendarId, from, to);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "응답 코드: " + response.getStatusCode());
        }
        return toDtoList(response.getBody());
    }

    /**
     * 구글 캘린더 이벤트 조회 엔드포인트를 호출한다.
     */
    private ResponseEntity<GoogleEventsResponse> fetchEventsFromApi(String accessToken, String calendarId, Instant from, Instant to) {
        String url = UriComponentsBuilder.fromHttpUrl(CALENDAR_BASE_URL + "/calendars/{calendarId}/events")
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .queryParam("timeMin", from.truncatedTo(ChronoUnit.SECONDS))
                .queryParam("timeMax", to.truncatedTo(ChronoUnit.SECONDS))
                .buildAndExpand(calendarId)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GoogleEventsResponse.class
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }
    }

    /**
     * 외부 API 응답을 도메인 DTO 목록으로 전환한다.
     */
    private List<GoogleCalendarEventDto> toDtoList(GoogleEventsResponse response) {
        if (response.items() == null) {
            return List.of();
        }
        Instant fallback = fallbackStart();
        return response.items().stream()
                .map(item -> toDto(item, fallback))
                .collect(Collectors.toList());
    }

    /**
     * GoogleEventItem을 GoogleCalendarEventDto로 변환한다.
     */
    private GoogleCalendarEventDto toDto(GoogleEventItem item, Instant fallback) {
        Instant start = item.start() != null ? item.start().instant() : fallback;
        Instant end = item.end() != null ? item.end().instant() : start;
        return new GoogleCalendarEventDto(
                item.id(),
                item.summary(),
                start,
                end
        );
    }

    /**
     * 조회 실패 시 사용할 UTC 기준 예비 시작 시간을 생성한다.
     */
    private Instant fallbackStart() {
        return Instant.now();
    }

    /**
     * AI 응답과 원본 요청을 조합해 캘린더 생성용 Payload를 만든다.
     */
    private GoogleCalendarEventInsertRequest buildEventInsertRequest(GoogleCalendarCreateEventRequest request, GoogleCalendarAiResponse aiResponse) {
        OffsetDateTime start = aiResponse.startAt() != null ? aiResponse.startAt() : request.startAt();
        OffsetDateTime end = aiResponse.endAt() != null ? aiResponse.endAt() : request.endAt();
        String summary = buildSummary(request);
        String description = describeAiBenefits(request, aiResponse);
        return new GoogleCalendarEventInsertRequest(
                summary,
                description,
                new GoogleEventDateTime(start),
                new GoogleEventDateTime(end)
        );
    }

    /**
     * summary는 카테고리와 브랜드를 # 구분자로 연결하여 생성한다.
     */
    private String buildSummary(GoogleCalendarCreateEventRequest request) {
        return "#" + request.category() + "#" + request.brand();
    }

    /**
     * AI가 전달한 혜택 메시지와 기본 정보를 하나의 설명으로 정리한다.
     */
    private String describeAiBenefits(GoogleCalendarCreateEventRequest request, GoogleCalendarAiResponse aiResponse) {
        StringBuilder builder = new StringBuilder();
        builder.append("카테고리: ").append(request.category()).append("\n");
        builder.append("브랜드: ").append(request.brand()).append("\n");
        builder.append("기간: ").append(formatRange(aiResponse)).append("\n");
        builder.append("AI 혜택 요약: ").append(aiResponse.benefitDescription());
        return builder.toString();
    }

    /**
     * 제공된 AI 응답의 기간 문자열을 생성한다.
     */
    private String formatRange(GoogleCalendarAiResponse aiResponse) {
        OffsetDateTime start = aiResponse.startAt();
        OffsetDateTime end = aiResponse.endAt();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        if (start != null && end != null) {
            return start.format(formatter) + " ~ " + end.format(formatter);
        }
        return "기간 정보 없음";
    }

    /**
     * 실제 Google Calendar API에 이벤트 등록 요청을 보낸다.
     */
    private GoogleCalendarEventDto insertEvent(String accessToken, String calendarId, GoogleCalendarEventInsertRequest request) {
        String url = CALENDAR_BASE_URL + "/calendars/{calendarId}/events";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        ResponseEntity<GoogleCalendarCreatedEventResponse> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    GoogleCalendarCreatedEventResponse.class,
                    calendarId
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "응답 코드: " + response.getStatusCode());
        }
        return toDtoFromCreated(response.getBody());
    }

    /**
     * 생성 결과를 DTO로 변환한다.
     */
    private GoogleCalendarEventDto toDtoFromCreated(GoogleCalendarCreatedEventResponse response) {
        Instant start = response.start() != null ? response.start().instant() : fallbackStart();
        Instant end = response.end() != null ? response.end().instant() : start;
        return new GoogleCalendarEventDto(response.id(), response.summary(), start, end);
    }

    /**
     * JJDC 캘린더 ID를 반환하며 없으면 생성한다.
     */
    private String ensureJjdcCalendarId(String accessToken) {
        Optional<String> existing = findCalendarId(accessToken);
        return existing.orElseGet(() -> createJjdcCalendar(accessToken));
    }

    /**
     * 사용자의 캘린더 목록을 조회하여 JJDC 캘린더 ID를 찾는다.
     */
    private Optional<String> findCalendarId(String accessToken) {
        String url = CALENDAR_BASE_URL + "/users/me/calendarList";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<GoogleCalendarListResponse> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GoogleCalendarListResponse.class
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "캘린더 목록 조회 실패");
        }
        if (response.getBody().items() == null) {
            return Optional.empty();
        }
        return response.getBody().items().stream()
                .filter(item -> JJDC_CALENDAR_SUMMARY.equals(item.summary()))
                .map(GoogleCalendarListItem::id)
                .findFirst();
    }

    /**
     * JJDC 캘린더가 없을 경우 새로 생성한다.
     */
    private String createJjdcCalendar(String accessToken) {
        String url = CALENDAR_BASE_URL + "/calendars";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "summary", JJDC_CALENDAR_SUMMARY,
                "timeZone", DEFAULT_TIMEZONE.getId()
        );
        ResponseEntity<GoogleCalendarCreateResponse> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    GoogleCalendarCreateResponse.class
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "JJDC 캘린더 생성 실패");
        }
        return response.getBody().id();
    }

    /**
     * 저장된 토큰으로부터 재사용 가능한 Access Token을 반환한다.
     */
    private String ensureAccessToken(GoogleOAuthToken token) {
        Instant expiresAt = token.getAccessTokenExpiredAt();
        if (expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return token.getAccessToken();
        }
        return refreshAccessToken(token);
    }

    /**
     * Refresh Token을 사용해 Google Access Token을 재발급한다.
     */
    private String refreshAccessToken(GoogleOAuthToken token) {
        if (token.getRefreshToken() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", token.getRefreshToken());

        ResponseEntity<GoogleTokenRefreshResponse> response;
        try {
            response = restTemplate.exchange(
                    TOKEN_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    GoogleTokenRefreshResponse.class
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "token 재발급 실패");
        }
        GoogleTokenRefreshResponse payload = response.getBody();
        Instant expiresAt = Instant.now().plusSeconds(payload.expiresIn());
        googleOAuthTokenService.updateAccessToken(token, payload.accessToken(), expiresAt);
        return payload.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleEventsResponse(List<GoogleEventItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleEventItem(String id, String summary, GoogleEventTime start, GoogleEventTime end) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleEventTime(String dateTime, String date) {
        Instant instant() {
            return dateTime != null ? Instant.parse(dateTime) : Instant.parse(date + "T00:00:00Z");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenRefreshResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarListResponse(List<GoogleCalendarListItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarListItem(String id, String summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarCreateResponse(String id, String summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleCalendarCreatedEventResponse(String id, String summary, GoogleEventTime start, GoogleEventTime end) {
    }

    private record GoogleCalendarEventInsertRequest(String summary, String description, GoogleEventDateTime start, GoogleEventDateTime end) {
    }

    private record GoogleEventDateTime(OffsetDateTime dateTime) {
    }
}
