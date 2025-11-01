package com.tbs.dto.move;

import com.tbs.enums.PlayerSymbol;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMoveRequest(
        @Min(value = 0, message = "Row must be non-negative")
        int row,

        @Min(value = 0, message = "Col must be non-negative")
        int col,

        @NotNull(message = "Player symbol is required")
        PlayerSymbol playerSymbol
) {}

