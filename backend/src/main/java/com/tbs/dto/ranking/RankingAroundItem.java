package com.tbs.dto.ranking;

public record RankingAroundItem(
        long rankPosition,
        long userId,
        String username,
        long totalPoints,
        int gamesPlayed,
        int gamesWon
) {}

