package com.tbs.dto.move;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.GameStatus;
import com.tbs.enums.PlayerSymbol;
import java.time.Instant;

public record BotMoveResponse(
        long moveId,
        long gameId,
        int row,
        int col,
        PlayerSymbol playerSymbol,
        int moveOrder,
        Instant createdAt,
        BoardState boardState,
        GameStatus gameStatus,
        WinnerInfo winner
) {}

