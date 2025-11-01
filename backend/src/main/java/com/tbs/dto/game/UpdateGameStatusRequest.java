package com.tbs.dto.game;

import com.tbs.enums.GameStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGameStatusRequest(
        @NotNull(message = "Status is required")
        GameStatus status
) {}

