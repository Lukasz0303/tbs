package com.tbs.dto.auth;

public record RegisterResponse(
        String userId,
        String username,
        String email,
        boolean isGuest,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        String authToken
) {
    public RegisterResponse {
        isGuest = false;
    }
}

