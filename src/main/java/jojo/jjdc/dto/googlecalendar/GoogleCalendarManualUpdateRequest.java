package jojo.jjdc.dto.googlecalendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record GoogleCalendarManualUpdateRequest(

        @Schema(description = "업데이트할 Google Calendar 이벤트 ID", example = "event-abc-123")
        String eventId,

        @Schema(description = "새로운 일정 시작 시각(ISO-8601)", example = "2025-12-20T09:00:00Z")
        OffsetDateTime startAt,

        @Schema(description = "캘린더 본문으로 덮어쓸 혜택 설명", example = "AI 추천 혜택 내용을 직접 입력")
        String suggest
) { }
