package com.tbs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlayerSymbol {
    X("x"),
    O("o");

    private final String value;

    PlayerSymbol(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PlayerSymbol fromValue(String value) {
        for (PlayerSymbol symbol : PlayerSymbol.values()) {
            if (symbol.value.equals(value)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("Unknown player symbol: " + value);
    }
}

