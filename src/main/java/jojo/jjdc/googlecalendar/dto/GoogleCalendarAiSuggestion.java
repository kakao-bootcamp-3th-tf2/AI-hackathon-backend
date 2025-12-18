package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record GoogleCalendarAiSuggestion(

        @Schema(description = "AI가 추천한 혜택의 설명", example = "카드 혜택 사용시 2% 캐시백")
        String suggest,

        @Schema(description = "추천 혜택이 적용되는 시작 시각", example = "2025-12-20T09:00:00Z")
        OffsetDateTime startAt,

        @Schema(description = "추천 혜택이 종료되는 시각", example = "2025-12-20T11:00:00Z")
        OffsetDateTime endAt
) {
}
