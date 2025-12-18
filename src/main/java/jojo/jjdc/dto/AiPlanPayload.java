package jojo.jjdc.dto;

import java.time.OffsetDateTime;

public record AiPlanPayload(
        OffsetDateTime dateTime,
        String brand,
        String category
) {
}
