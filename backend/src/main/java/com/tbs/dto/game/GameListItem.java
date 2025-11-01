package com.tbs.dto.game;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import java.time.Instant;

public record GameListItem(
        long gameId,
        GameType gameType,
        BoardSize boardSize,
        GameStatus status,
        String player1Username,
        String player2Username,
        String winnerUsername,
        BotDifficulty botDifficulty,
        int totalMoves,
        Instant createdAt,
        Instant lastMoveAt,
        Instant finishedAt
) {}

