package com.tbs.dto.game;

import com.tbs.enums.GameStatus;
import java.time.Instant;

public record UpdateGameStatusResponse(
        long gameId,
        GameStatus status,
        Instant updatedAt
) {}

