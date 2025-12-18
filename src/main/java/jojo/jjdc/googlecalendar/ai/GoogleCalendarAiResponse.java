package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.ai.dto.AiRecommendationItem;

public record GoogleCalendarAiResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String benefitDescription,
        List<AiRecommendationItem> suggestions
) {
}
