package jojo.jjdc.googlecalendar.client;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient {

    private static final String CALENDAR_BASE_URL = "https://www.googleapis.com/calendar/v3";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final RestTemplate restTemplate;

    /**
     * 지정된 캘린더에서 이벤트 목록을 조회한다.
     */
    public GoogleEventsResponse fetchEvents(String accessToken, String calendarId, Instant from, Instant to) {
        Instant fromInstant = from.truncatedTo(ChronoUnit.SECONDS);
        Instant toInstant = to.truncatedTo(ChronoUnit.SECONDS);
        String url = UriComponentsBuilder.fromHttpUrl(CALENDAR_BASE_URL + "/calendars/{calendarId}/events")
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .queryParam("timeMin", fromInstant)
                .queryParam("timeMax", toInstant)
                .buildAndExpand(calendarId)
                .encode()
                .toUriString();
        HttpHeaders headers = createAuthHeaders(accessToken);
        return execute(url, HttpMethod.GET, new HttpEntity<>(headers), GoogleEventsResponse.class);
    }

    /**
     * 단일 이벤트의 상세 정보를 조회한다.
     */
    public GoogleCalendarEventDetailResponse fetchEventDetail(String accessToken, String calendarId, String eventId) {
        String url = CALENDAR_BASE_URL + "/calendars/{calendarId}/events/{eventId}";
        HttpHeaders headers = createAuthHeaders(accessToken);
        return execute(url, HttpMethod.GET, new HttpEntity<>(headers), GoogleCalendarEventDetailResponse.class, calendarId, eventId);
    }

    /**
     * 일정 세부 내용을 갱신한다.
     */
    public GoogleCalendarCreatedEventResponse patchEvent(String accessToken, String calendarId, String eventId, GoogleCalendarEventPatchRequest request) {
        String url = CALENDAR_BASE_URL + "/calendars/{calendarId}/events/{eventId}";
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return execute(
                url,
                HttpMethod.PATCH,
                new HttpEntity<>(request, headers),
                GoogleCalendarCreatedEventResponse.class,
                calendarId,
                eventId
        );
    }

    /**
     * 새로운 일정을 생성한다.
     */
    public GoogleCalendarCreatedEventResponse insertEvent(String accessToken, String calendarId, GoogleCalendarEventInsertRequest request) {
        String url = CALENDAR_BASE_URL + "/calendars/{calendarId}/events";
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return execute(url, HttpMethod.POST, new HttpEntity<>(request, headers), GoogleCalendarCreatedEventResponse.class, calendarId);
    }

    /**
     * 캘린더 목록을 조회해서 특정 summary를 갖는 캘린더 ID를 탐색한다.
     */
    public Optional<String> findCalendarId(String accessToken, String summary) {
        GoogleCalendarListResponse response = fetchCalendarList(accessToken);
        if (response == null || response.items() == null) {
            return Optional.empty();
        }
        return response.items().stream()
                .filter(item -> summary.equals(item.summary()))
                .map(GoogleCalendarListItem::id)
                .findFirst();
    }

    /**
     * 새로운 캘린더를 생성하고 ID를 반환한다.
     */
    public String createCalendar(String accessToken, String summary, String timezone) {
        String url = CALENDAR_BASE_URL + "/calendars";
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "summary", summary,
                "timeZone", timezone
        );
        GoogleCalendarCreateResponse response = execute(url, HttpMethod.POST, new HttpEntity<>(body, headers), GoogleCalendarCreateResponse.class);
        return response.id();
    }

    /**
     * Refresh token을 사용해 엑세스 토큰을 재발급 받는다.
     */
    public GoogleTokenRefreshResponse refreshAccessToken(String clientId, String clientSecret, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        return execute(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, headers), GoogleTokenRefreshResponse.class);
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private <T> T execute(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType, Object... uriVars) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType, uriVars);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "응답 코드: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }
    }

    private GoogleCalendarListResponse fetchCalendarList(String accessToken) {
        String url = CALENDAR_BASE_URL + "/users/me/calendarList";
        return execute(url, HttpMethod.GET, new HttpEntity<>(createAuthHeaders(accessToken)), GoogleCalendarListResponse.class);
    }

    public record GoogleEventsResponse(List<GoogleEventItem> items) {
    }

    public record GoogleEventItem(String id, String summary, GoogleEventTime start, GoogleEventTime end, String description) {
    }

    public record GoogleEventTime(String dateTime, String date) {
        public Instant instant() {
            return dateTime != null ? Instant.parse(dateTime) : Instant.parse(date + "T00:00:00Z");
        }
    }

    public record GoogleCalendarEventInsertRequest(String summary, String description, GoogleEventDateTime start, GoogleEventDateTime end) {
    }

    public record GoogleEventDateTime(Instant dateTime, String timeZone, String date) {
    }

    public record GoogleCalendarCreatedEventResponse(String id, String summary, String description, GoogleEventTime start, GoogleEventTime end) {
    }

    public record GoogleCalendarEventDetailResponse(String id, String summary, String description, GoogleEventTime start, GoogleEventTime end) {
    }

    public record GoogleCalendarEventPatchRequest(String summary, String description, GoogleEventDateTime start, GoogleEventDateTime end) {
    }

    public record GoogleCalendarListResponse(List<GoogleCalendarListItem> items) {
    }

    public record GoogleCalendarListItem(String id, String summary) {
    }

    public record GoogleCalendarCreateResponse(String id, String summary) {
    }

    public record GoogleTokenRefreshResponse(
            String accessToken,
            Long expiresIn
    ) {
    }
}
