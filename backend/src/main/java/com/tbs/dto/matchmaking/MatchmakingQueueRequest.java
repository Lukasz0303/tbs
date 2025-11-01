package com.tbs.dto.matchmaking;

import com.tbs.enums.BoardSize;
import jakarta.validation.constraints.NotNull;

public record MatchmakingQueueRequest(
        @NotNull(message = "Board size is required")
        BoardSize boardSize
) {}

