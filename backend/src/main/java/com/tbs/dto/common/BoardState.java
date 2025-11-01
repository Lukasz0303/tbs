package com.tbs.dto.common;

import java.util.Arrays;

public record BoardState(String[][] state) {
    public static BoardState empty(int size) {
        String[][] emptyState = new String[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(emptyState[i], "");
        }
        return new BoardState(emptyState);
    }

    public boolean isEmpty(int row, int col) {
        if (row < 0 || row >= state.length || col < 0 || col >= state[0].length) {
            return false;
        }
        String cell = state[row][col];
        return cell == null || cell.isEmpty();
    }
}

