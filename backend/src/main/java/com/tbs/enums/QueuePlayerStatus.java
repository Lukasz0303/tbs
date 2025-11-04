package com.tbs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QueuePlayerStatus {
    WAITING("waiting"),
    MATCHED("matched"),
    PLAYING("playing");

    private final String value;

    QueuePlayerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QueuePlayerStatus fromValue(String value) {
        for (QueuePlayerStatus status : QueuePlayerStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown queue player status: " + value);
    }
}
