package jojo.jjdc.ai.dto;

import java.util.List;

public record AiRecommendationResponse(
        int code,
        String message,
        List<AiRecommendationItem> data
) {
}
