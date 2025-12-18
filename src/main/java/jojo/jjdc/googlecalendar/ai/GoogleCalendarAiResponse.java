package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.googlecalendar.ai.AiServerResponse.AiSuggestion;

public record GoogleCalendarAiResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String benefitDescription,
        List<AiSuggestion> suggestions
) {
}
