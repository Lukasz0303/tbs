package com.tbs.dto.matchmaking;

import com.tbs.enums.BoardSize;
import com.tbs.enums.QueuePlayerStatus;

import java.time.Instant;

public record PlayerQueueStatus(
        Long userId,
        String username,
        BoardSize boardSize,
        QueuePlayerStatus status,
        Instant joinedAt,
        Long matchedWith,
        String matchedWithUsername,
        Long gameId,
        Boolean isMatched,
        Long score
) {}
