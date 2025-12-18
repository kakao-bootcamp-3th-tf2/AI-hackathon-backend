package jojo.jjdc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarCreateEventRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarEventDto;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarEventsResponse;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarManualUpdateRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarSuggestRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarSuggestResponse;
import jojo.jjdc.dto.NotityDto;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.service.googlecalendar.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Google Calendar", description = "Google Calendar 연동 API")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/events")
    @Operation(summary = "기본 캘린더 이벤트 조회", description = "연결된 구글 계정의 primary 캘린더에서 일정 목록을 조회한다.")
    public ResponseEntity<APIResponse<GoogleCalendarEventsResponse>> getEvents(
            @AuthenticationPrincipal MemberPrincipal principal,

            @Parameter(description = "조회 시작 시각(ISO-8601). 미지정 시 오늘 00:00Z", example = "2025-12-18T00:00:00Z")
            @RequestParam(value = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,

            @Parameter(description = "조회 종료 시각(ISO-8601). 미지정 시 시작 시각 + 1일", example = "2025-12-19T00:00:00Z")
            @RequestParam(value = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        List<GoogleCalendarEventDto> events = googleCalendarService.getEvents(principal, from, to);

        GoogleCalendarEventsResponse response = new GoogleCalendarEventsResponse(events);
        return ResponseEntity
                .status(SuccessCode.GOOGLE_EVENTS_FETCHED.getStatus())
                .body(APIResponse.ok(SuccessCode.GOOGLE_EVENTS_FETCHED, response));
    }

    @PostMapping("/events")
    @Operation(summary = "JJDC 캘린더 일정 등록", description = "카테고리/브랜드/기간에 맞춰 AI 추천 혜택을 포함한 일정을 생성하고 알람으로 저장합니다.")
    public ResponseEntity<APIResponse<NotityDto>> createEvent(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody GoogleCalendarCreateEventRequest request
    ) {
        NotityDto notity = googleCalendarService.createEvent(principal, request);
        return ResponseEntity
                .status(SuccessCode.GOOGLE_EVENT_CREATED.getStatus())
                .body(APIResponse.ok(SuccessCode.GOOGLE_EVENT_CREATED, notity));
    }

    @PatchMapping("/events/suggest")
    @Operation(summary = "여러 일정 AI 설명 추가", description = "일정 ID 목록을 받아, 카테고리/브랜드를 추출하고 AI 응답을 설명에 새로 쓰기 합니다.")
    public ResponseEntity<APIResponse<List<GoogleCalendarSuggestResponse>>> appendSuggest(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody GoogleCalendarSuggestRequest request
    ) {
        List<GoogleCalendarSuggestResponse> results = googleCalendarService.appendSuggest(principal, request.needSuggestList());
        return ResponseEntity
                .status(SuccessCode.GOOGLE_EVENT_UPDATED.getStatus())
                .body(APIResponse.ok(SuccessCode.GOOGLE_EVENT_UPDATED, results));
    }

    @PutMapping("/events/manual")
    @Operation(summary = "일정 수동 덮어쓰기", description = "eventId/startAt/[endAt]/suggest를 받아 일정 기간과 설명을 덮어쓰고 알람을 저장합니다. endAt은 없거나 시작보다 앞설 경우 startAt+1분으로 보정합니다.")
    public ResponseEntity<APIResponse<NotityDto>> overwriteEvent(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody GoogleCalendarManualUpdateRequest request
    ) {
        NotityDto notity = googleCalendarService.overwriteEvent(principal, request);
        return ResponseEntity
                .status(SuccessCode.GOOGLE_EVENT_UPDATED.getStatus())
                .body(APIResponse.ok(SuccessCode.GOOGLE_EVENT_UPDATED, notity));
    }
}
