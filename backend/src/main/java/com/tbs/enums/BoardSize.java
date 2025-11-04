package com.tbs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BoardSize {
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int value;

    BoardSize(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static BoardSize fromValue(int value) {
        for (BoardSize size : BoardSize.values()) {
            if (size.value == value) {
                return size;
            }
        }
        throw new IllegalArgumentException("Unknown board size: " + value);
    }

    @JsonCreator
    public static BoardSize fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int intValue = Integer.parseInt(value);
            return fromValue(intValue);
        } catch (NumberFormatException e) {
            try {
                return BoardSize.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown board size: " + value);
            }
        }
    }
}

