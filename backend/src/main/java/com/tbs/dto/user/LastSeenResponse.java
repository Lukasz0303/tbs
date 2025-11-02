package com.tbs.dto.user;

import java.time.Instant;

public record LastSeenResponse(
        String message,
        Instant lastSeenAt
) {}

