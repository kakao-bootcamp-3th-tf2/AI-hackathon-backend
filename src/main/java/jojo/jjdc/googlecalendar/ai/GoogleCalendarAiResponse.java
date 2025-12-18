package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;

public record GoogleCalendarAiResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String benefitDescription
) {
}
