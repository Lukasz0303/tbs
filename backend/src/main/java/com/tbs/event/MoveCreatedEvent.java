package com.tbs.event;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.GameStatus;
import com.tbs.enums.PlayerSymbol;

public record MoveCreatedEvent(
        Long gameId,
        Long userId,
        Long moveId,
        int row,
        int col,
        PlayerSymbol playerSymbol,
        BoardState boardState,
        PlayerSymbol currentPlayerSymbol,
        GameStatus gameStatus
) {}

