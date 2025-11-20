package com.tbs.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record UpdateUserRequest(
        @Nullable
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @Nullable
        @Min(value = 1, message = "Avatar must be between 1 and 6")
        @Max(value = 6, message = "Avatar must be between 1 and 6")
        Integer avatar
) {}

