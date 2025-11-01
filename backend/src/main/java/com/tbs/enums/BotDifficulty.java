package com.tbs.enums;

public enum BotDifficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String value;

    BotDifficulty(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BotDifficulty fromValue(String value) {
        for (BotDifficulty difficulty : BotDifficulty.values()) {
            if (difficulty.value.equals(value)) {
                return difficulty;
            }
        }
        throw new IllegalArgumentException("Unknown bot difficulty: " + value);
    }
}

