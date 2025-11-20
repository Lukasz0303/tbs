package com.tbs.dto.auth;

public record LoginResponse(
        String userId,
        String username,
        String email,
        boolean isGuest,
        Integer avatar,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        String authToken
) {
    public LoginResponse {
        isGuest = false;
    }
}

