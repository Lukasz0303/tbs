package com.tbs.dto.health;

public record HealthResponse(
        HealthStatus status,
        HealthComponents components
) {
    public record HealthComponents(
            HealthComponent db,
            HealthComponent redis,
            HealthComponent websocket
    ) {}
}

