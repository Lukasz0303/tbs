package com.tbs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BotDifficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String value;

    BotDifficulty(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BotDifficulty fromValue(String value) {
        for (BotDifficulty difficulty : BotDifficulty.values()) {
            if (difficulty.value.equals(value)) {
                return difficulty;
            }
        }
        throw new IllegalArgumentException("Unknown bot difficulty: " + value);
    }
}

