package jojo.jjdc.dto.googlecalendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jojo.jjdc.dto.NotityDto;

public record GoogleCalendarSuggestResponse(

        @Schema(description = "AI 설명을 추가할 대상 Google Calendar 이벤트 ID", example = "7akl2545mtklm5o3o5gh72lo3k")
        String eventId,

        @Schema(description = "AI 설명 결과 저장된 알람. 실패 시 null")
        NotityDto notity
) { }
