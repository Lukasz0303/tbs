package com.tbs.dto.matchmaking;

import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import java.time.Instant;

public record ChallengeResponse(
        long gameId,
        GameType gameType,
        BoardSize boardSize,
        long player1Id,
        long player2Id,
        GameStatus status,
        Instant createdAt
) {
    public ChallengeResponse {
        gameType = GameType.PVP;
    }
}

