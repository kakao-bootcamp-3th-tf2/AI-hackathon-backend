package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoogleCalendarSuggestResponse(

        @Schema(description = "AI 설명을 추가할 대상 Google Calendar 이벤트 ID", example = "7akl2545mtklm5o3o5gh72lo3k")
        String eventId,

        @Schema(description = "AI 설명이 붙은 이벤트 정보. 실패 시 빈 문자열(\"\"), 정상 처리된 경우 `GoogleCalendarEventDto` 객체가 담김")
        Object event
) {  }
