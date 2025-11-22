package com.tbs.dto.auth;

public record LoginResponse(
        Long userId,
        String username,
        String email,
        boolean isGuest,
        Integer avatar,
        long totalPoints,
        int gamesPlayed,
        int gamesWon
) {
    public LoginResponse {
        isGuest = false;
    }
}

