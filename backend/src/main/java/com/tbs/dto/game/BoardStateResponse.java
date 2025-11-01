package com.tbs.dto.game;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.BoardSize;
import com.tbs.enums.PlayerSymbol;

public record BoardStateResponse(
        BoardState boardState,
        BoardSize boardSize,
        int totalMoves,
        LastMove lastMove
) {
    public record LastMove(
            int row,
            int col,
            PlayerSymbol playerSymbol,
            int moveOrder
    ) {}
}

