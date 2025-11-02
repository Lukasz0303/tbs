package com.tbs.dto.game;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.GameType;
import jakarta.validation.constraints.NotNull;

public record CreateGameRequest(
        @NotNull(message = "Game type is required")
        GameType gameType,

        @NotNull(message = "Board size is required")
        Integer boardSize,

        BotDifficulty botDifficulty
) {}

