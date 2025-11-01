package com.tbs.enums;

public enum BoardSize {
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int value;

    BoardSize(int value) {
        this.value = value;
    }

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
}

