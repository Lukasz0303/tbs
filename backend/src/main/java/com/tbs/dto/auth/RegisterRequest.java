package com.tbs.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(example = "user@example.com", description = "User email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(example = "securePassword123", description = "User password (minimum 8 characters)")
        String password,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Schema(example = "john_doe", description = "Username (3-50 characters)")
        String username
) {}

