package jojo.jjdc.service.ai;

import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.dto.AiRecommendationItem;

public record GoogleCalendarAiResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String benefitDescription,
        List<AiRecommendationItem> suggestions
) {
}
