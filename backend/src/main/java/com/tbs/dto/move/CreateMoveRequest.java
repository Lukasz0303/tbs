package com.tbs.dto.move;

import com.tbs.enums.PlayerSymbol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMoveRequest(
        @Min(value = 0, message = "Row must be non-negative")
        @Schema(example = "0", description = "Row index (0-based)")
        int row,

        @Min(value = 0, message = "Col must be non-negative")
        @Schema(example = "0", description = "Column index (0-based)")
        int col,

        @NotNull(message = "Player symbol is required")
        @Schema(example = "X", description = "Player symbol: X or O")
        PlayerSymbol playerSymbol
) {}

