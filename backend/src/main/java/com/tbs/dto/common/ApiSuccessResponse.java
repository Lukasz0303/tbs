package com.tbs.dto.common;

import java.time.Instant;

public record ApiSuccessResponse<T>(
        T data,
        Instant timestamp,
        String status
) {
    public ApiSuccessResponse(T data) {
        this(data, Instant.now(), "success");
    }
}

