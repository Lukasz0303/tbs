package com.tbs.dto.matchmaking;

import com.tbs.enums.BoardSize;
import jakarta.validation.constraints.NotNull;

public record ChallengeRequest(
        @NotNull(message = "Board size is required")
        BoardSize boardSize
) {}

