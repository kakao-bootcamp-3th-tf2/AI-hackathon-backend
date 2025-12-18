package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record GoogleCalendarCreateEventRequest(

        @Schema(description = "캘린더 일정의 카테고리 이름(프론트 식별자)", example = "금융 이벤트")
        String category,

        @Schema(description = "대상 브랜드 이름", example = "JJDC Money")
        String brand,

        @Schema(description = "일정 시작 시각(ISO-8601)", example = "2025-12-20T09:00:00Z")
        OffsetDateTime startAt,

        @Schema(description = "일정 종료 시각(ISO-8601)", example = "2025-12-20T11:00:00Z")
        OffsetDateTime endAt
) {
}
