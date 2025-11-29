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
    public static BoardSize fromValue(Object value) {
        if (value == null) {
            return null;
        }
        
        int intValue;
        if (value instanceof Number) {
            intValue = ((Number) value).intValue();
        } else {
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) {
                return null;
            }
            try {
                intValue = Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                try {
                    return BoardSize.valueOf(stringValue.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Unknown board size: " + value, ex);
                }
            }
        }
        
        return fromValue(intValue);
    }
}

