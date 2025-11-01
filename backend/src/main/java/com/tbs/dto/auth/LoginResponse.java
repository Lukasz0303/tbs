package com.tbs.dto.auth;

public record LoginResponse(
        String userId,
        String username,
        String email,
        boolean isGuest,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        String authToken
) {
    public LoginResponse {
        isGuest = false;
    }
}

