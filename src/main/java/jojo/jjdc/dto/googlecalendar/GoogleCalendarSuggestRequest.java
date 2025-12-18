package jojo.jjdc.dto.googlecalendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GoogleCalendarSuggestRequest(

        @Schema(description = "AI 설명을 받고 싶은 일정 ID 리스트", example = "[\"event1\",\"event2\"]")
        List<String> needSuggestList
) { }
