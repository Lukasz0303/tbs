package com.tbs.dto.ranking;

import java.time.Instant;

public record RankingDetailResponse(
        long rankPosition,
        long userId,
        String username,
        long totalPoints,
        int gamesPlayed,
        int gamesWon,
        Instant createdAt
) {}

