package com.tbs.enums;

public enum PlayerSymbol {
    X("x"),
    O("o");

    private final String value;

    PlayerSymbol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PlayerSymbol fromValue(String value) {
        for (PlayerSymbol symbol : PlayerSymbol.values()) {
            if (symbol.value.equals(value)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("Unknown player symbol: " + value);
    }
}

