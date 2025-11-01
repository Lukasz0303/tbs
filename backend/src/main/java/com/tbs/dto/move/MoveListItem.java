package com.tbs.dto.move;

import com.tbs.enums.PlayerSymbol;
import java.time.Instant;

public record MoveListItem(
        long moveId,
        int row,
        int col,
        PlayerSymbol playerSymbol,
        int moveOrder,
        Long playerId,
        String playerUsername,
        Instant createdAt
) {}

