package com.tbs.dto.game;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.BotDifficulty;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import java.time.Instant;

public record CreateGameResponse(
        long gameId,
        GameType gameType,
        BoardSize boardSize,
        long player1Id,
        Long player2Id,
        BotDifficulty botDifficulty,
        GameStatus status,
        PlayerSymbol currentPlayerSymbol,
        Instant createdAt,
        BoardState boardState
) {}

