package com.tbs.dto.matchmaking;

import com.tbs.dto.common.MessageResponse;

public record MatchmakingQueueResponse(
        String message,
        int estimatedWaitTime
) implements MessageResponse {}

