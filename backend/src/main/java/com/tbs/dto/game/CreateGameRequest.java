package com.tbs.dto.game;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.GameType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateGameRequest(
        @NotNull(message = "Game type is required")
        GameType gameType,

        @NotNull(message = "Board size is required")
        @Min(value = 3, message = "Board size must be at least 3")
        @Max(value = 5, message = "Board size must be at most 5")
        Integer boardSize,

        BotDifficulty botDifficulty
) {}

