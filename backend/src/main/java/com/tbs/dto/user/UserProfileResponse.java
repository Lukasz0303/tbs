package com.tbs.dto.user;

import java.time.Instant;

public record UserProfileResponse(
        long userId,
        String username,
        boolean isGuest,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        Instant createdAt
) {}

