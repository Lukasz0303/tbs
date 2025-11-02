package com.tbs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GameStatus {
    WAITING("waiting"),
    IN_PROGRESS("in_progress"),
    FINISHED("finished"),
    ABANDONED("abandoned"),
    DRAW("draw");

    private final String value;

    GameStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static GameStatus fromValue(String value) {
        for (GameStatus status : GameStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown game status: " + value);
    }
}

