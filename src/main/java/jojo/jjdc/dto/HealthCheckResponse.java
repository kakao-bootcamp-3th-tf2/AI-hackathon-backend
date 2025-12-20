package jojo.jjdc.dto;

import java.time.Instant;

public record HealthCheckResponse(String status, Instant timestamp) {

    public static HealthCheckResponse up() {
        return new HealthCheckResponse("UP", Instant.now());
    }
}
