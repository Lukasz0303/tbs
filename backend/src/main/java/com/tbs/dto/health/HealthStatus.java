package com.tbs.dto.health;

public enum HealthStatus {
    UP("UP"),
    DOWN("DOWN");

    private final String value;

    HealthStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

