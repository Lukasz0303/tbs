package com.tbs.dto.guest;

import java.time.Instant;

public record GuestResponse(
        long userId,
        boolean isGuest,
        Integer avatar,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        Instant createdAt,
        String authToken
) {
    public GuestResponse {
        isGuest = true;
    }
}

