package com.tbs.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateAvatarRequest(
        @NotNull(message = "Avatar is required")
        @Min(value = 1, message = "Avatar must be between 1 and 6")
        @Max(value = 6, message = "Avatar must be between 1 and 6")
        @Schema(example = "1", description = "Avatar ID (1-6)")
        Integer avatar
) {}

