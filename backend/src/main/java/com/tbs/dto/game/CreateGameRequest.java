package com.tbs.dto.game;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.GameType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateGameRequest(
        @NotNull(message = "Game type is required")
        @Schema(example = "VS_BOT", description = "Type of game: VS_BOT or PVP")
        GameType gameType,

        @NotNull(message = "Board size is required")
        @Min(value = 3, message = "Board size must be at least 3")
        @Max(value = 5, message = "Board size must be at most 5")
        @Schema(example = "3", description = "Board size (3x3, 4x4, or 5x5)")
        Integer boardSize,

        @Schema(example = "MEDIUM", description = "Bot difficulty (required for VS_BOT games): EASY, MEDIUM, or HARD")
        BotDifficulty botDifficulty
) {}

