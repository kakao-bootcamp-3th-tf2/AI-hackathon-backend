package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record GoogleCalendarEventDto(

        @Schema(description = "Google Calendar에서 발급한 이벤트 ID", example = "event-abc-123")
        String id,

        @Schema(description = "JJDC 포맷(#카테고리#브랜드)으로 생성된 제목", example = "#금융이벤트#JJDC Money")
        String summary,

        @Schema(description = "이벤트 시작 시각(UTC, ISO-8601)", example = "2025-12-20T09:00:00Z")
        Instant startAt,

        @Schema(description = "이벤트 종료 시각(UTC, ISO-8601)", example = "2025-12-20T11:00:00Z")
        Instant endAt,

        @Schema(description = "AI가 추천한 혜택 리스트")
        List<SuggestDto> suggestList,

        @Schema(description = "일정의 description/본문 내용")
        String content
) { }
