package jojo.jjdc.notity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import jojo.jjdc.googlecalendar.dto.SuggestDto;

public record NotityDto(

        @Schema(description = "알람(노티티) ID", example = "42")
        Long id,

        @Schema(description = "연결된 Google Calendar 이벤트 ID", example = "event-abc-123")
        String eventId,

        @Schema(description = "일정 제목", example = "#카테고리#브랜드")
        String summary,

        @Schema(description = "일정 시작 시각(UTC)", example = "2025-12-20T09:00:00Z")
        Instant startAt,

        @Schema(description = "일정 종료 시각(UTC)", example = "2025-12-20T11:00:00Z")
        Instant endAt,

        @Schema(description = "일정 본문/내용")
        String content,

        @Schema(description = "혜택 제안 목록")
        List<SuggestDto> suggestList
) {
}
