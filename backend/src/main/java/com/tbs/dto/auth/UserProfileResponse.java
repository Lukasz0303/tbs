package com.tbs.dto.auth;

import java.time.Instant;

public record UserProfileResponse(
        long userId,
        String username,
        boolean isGuest,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        Instant createdAt,
        Instant lastSeenAt
) {}

