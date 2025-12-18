package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoogleCalendarAppendRequest(

        @Schema(description = "AI로 분석할 일정의 카테고리(선택)", example = "금융 이벤트")
        String category,

        @Schema(description = "AI로 분석할 일정의 브랜드명(선택)", example = "JJDC Money")
        String brand
) { }
