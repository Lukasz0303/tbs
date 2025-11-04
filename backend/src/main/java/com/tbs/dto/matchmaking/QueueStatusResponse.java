package com.tbs.dto.matchmaking;

import java.util.List;

public record QueueStatusResponse(
        List<PlayerQueueStatus> players,
        Integer totalCount
) {}
