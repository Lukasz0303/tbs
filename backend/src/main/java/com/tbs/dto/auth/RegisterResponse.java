package com.tbs.dto.auth;

public record RegisterResponse(
        Long userId,
        String username,
        String email,
        boolean isGuest,
        Integer avatar,
        long totalPoints,
        int gamesPlayed,
        int gamesWon
) {
    public RegisterResponse {
        isGuest = false;
    }
}

