package com.tbs.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        @Schema(example = "user@example.com", description = "User email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Schema(example = "securePassword123", description = "User password (minimum 8 characters)")
        String password,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Schema(example = "john_doe", description = "Username (3-50 characters)")
        String username,

        @Nullable
        @Min(value = 1, message = "Avatar must be between 1 and 6")
        @Max(value = 6, message = "Avatar must be between 1 and 6")
        @Schema(example = "1", description = "Avatar ID (1-6), defaults to 1 if not provided")
        Integer avatar
) {}
