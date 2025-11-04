package com.tbs.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(example = "user@example.com", description = "User email address")
        String email,

        @NotBlank(message = "Password is required")
        @Schema(example = "securePassword123", description = "User password")
        String password
) {}

