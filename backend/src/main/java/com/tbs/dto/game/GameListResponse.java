package com.tbs.dto.game;

import com.tbs.dto.common.PaginatedResponse;

public record GameListResponse(
        java.util.List<GameListItem> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {}

