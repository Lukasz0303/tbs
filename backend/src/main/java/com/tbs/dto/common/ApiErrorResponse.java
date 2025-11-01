package com.tbs.dto.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        ErrorDetails error,
        Instant timestamp,
        String status
) {
    public record ErrorDetails(
            String code,
            String message,
            Map<String, Object> details
    ) {
        public ErrorDetails(String code, String message) {
            this(code, message, null);
        }
    }

    public ApiErrorResponse(ErrorDetails error) {
        this(error, Instant.now(), "error");
    }
}

