package jojo.jjdc.googlecalendar.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventDto;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarEventsResponse;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/google/calendar")
@RequiredArgsConstructor
@Tag(name = "Google Calendar", description = "Google Calendar 연동 API")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/primary/events")
    @Operation(summary = "기본 캘린더 이벤트 조회", description = "연결된 구글 계정의 primary 캘린더에서 일정 목록을 조회한다.")
    public ResponseEntity<APIResponse<GoogleCalendarEventsResponse>> primaryEvents(
            @AuthenticationPrincipal MemberPrincipal principal,

            @Parameter(description = "조회 시작 시각(ISO-8601). 미지정 시 오늘 00:00Z", example = "2025-12-18T00:00:00Z")
            @RequestParam(value = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,

            @Parameter(description = "조회 종료 시각(ISO-8601). 미지정 시 시작 시각 + 1일", example = "2025-12-19T00:00:00Z")
            @RequestParam(value = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<GoogleCalendarEventDto> events = googleCalendarService.getPrimaryEvents(
                principal.memberId(),
                from.toInstant(),
                to.toInstant()
        );

        GoogleCalendarEventsResponse response = new GoogleCalendarEventsResponse(events);
        return ResponseEntity
                .status(SuccessCode.GOOGLE_EVENTS_FETCHED.getStatus())
                .body(APIResponse.ok(SuccessCode.GOOGLE_EVENTS_FETCHED, response));
    }
}
