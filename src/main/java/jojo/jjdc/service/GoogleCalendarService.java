package jojo.jjdc.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventDto;
import jojo.jjdc.security.oauth.entity.GoogleOAuthToken;
import jojo.jjdc.security.oauth.service.GoogleOAuthTokenService;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
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

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public List<GoogleCalendarEventDto> getPrimaryEvents(Long memberId, Instant from, Instant to) {
        GoogleOAuthToken token = googleOAuthTokenService.getByMemberId(memberId);
        String accessToken = ensureAccessToken(token);
        String calendarId = ensureJjdcCalendarId(accessToken);

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
        ResponseEntity<GoogleEventsResponse> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GoogleEventsResponse.class
            );
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, ex.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.GOOGLE_CALENDAR_REQUEST_FAILED, "응답 코드: " + response.getStatusCode());
        }
        GoogleEventsResponse body = response.getBody();
        if (body.items() == null) {
            return List.of();
        }
        return body.items().stream()
                .map(item -> {
                    Instant start = item.start() != null ? item.start().instant() : fromFallback();
                    Instant end = item.end() != null ? item.end().instant() : start;
                    return new GoogleCalendarEventDto(
                            item.id(),
                            item.summary(),
                            start,
                            end
                    );
                })
                .collect(Collectors.toList());
    }

    private Instant fromFallback() {
        return Instant.now();
    }

    private String ensureJjdcCalendarId(String accessToken) {
        Optional<String> existing = findCalendarId(accessToken);
        return existing.orElseGet(() -> createJjdcCalendar(accessToken));
    }

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
}
