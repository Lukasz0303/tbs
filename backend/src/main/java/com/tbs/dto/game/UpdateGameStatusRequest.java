package com.tbs.dto.game;

import com.tbs.enums.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateGameStatusRequest(
        @NotNull(message = "Status is required")
        @Schema(example = "FINISHED", description = "New game status: FINISHED (surrender) or ABANDONED")
        GameStatus status
) {}

