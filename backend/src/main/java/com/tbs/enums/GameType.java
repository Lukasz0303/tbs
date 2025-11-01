package com.tbs.enums;

public enum GameType {
    VS_BOT("vs_bot"),
    PVP("pvp");

    private final String value;

    GameType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GameType fromValue(String value) {
        for (GameType type : GameType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown game type: " + value);
    }
}

