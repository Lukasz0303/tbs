package com.tbs.dto.matchmaking;

public record MatchmakingQueueResponse(
        String message,
        int estimatedWaitTime
) {}

