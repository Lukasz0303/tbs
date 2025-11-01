package com.tbs.dto.common;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {
    public static <T> PaginatedResponse<T> of(List<T> content, long totalElements, int totalPages, int size, int number) {
        boolean isFirst = number == 0;
        boolean isLast = number >= totalPages - 1;
        return new PaginatedResponse<>(content, totalElements, totalPages, size, number, isFirst, isLast);
    }
}

