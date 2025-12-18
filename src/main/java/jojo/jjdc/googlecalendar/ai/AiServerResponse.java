package jojo.jjdc.googlecalendar.ai;

import java.util.List;

public record AiServerResponse(
        String message,
        String code,
        List<AiSuggestion> data
) {
    public record AiSuggestion(
            String suggest,
            String fromDate,
            String toDate
    ) { }
}
