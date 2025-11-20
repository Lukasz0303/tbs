package com.tbs.dto.user;

import java.time.Instant;

public record UpdateUserResponse(
        long userId,
        String username,
        boolean isGuest,
        Integer avatar,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        Instant updatedAt
) {}

