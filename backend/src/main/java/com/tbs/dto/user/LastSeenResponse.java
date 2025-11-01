package com.tbs.dto.user;

import com.tbs.dto.common.MessageResponse;
import java.time.Instant;

public record LastSeenResponse(
        String message,
        Instant lastSeenAt
) implements MessageResponse {}

