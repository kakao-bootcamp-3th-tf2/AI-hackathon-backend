package jojo.jjdc.dto;

import java.util.List;

public record AiRecommendationRequest(
        UserPayload user,
        PlanPayload plan
) {
    public record UserPayload(String telecom, List<String> payments) { }
    public record PlanPayload(String dateTime, String brand, String category) { }
}
