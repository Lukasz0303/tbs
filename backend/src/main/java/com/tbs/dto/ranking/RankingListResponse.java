package com.tbs.dto.ranking;

import com.tbs.dto.common.PaginatedResponse;

public record RankingListResponse(
        java.util.List<RankingItem> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {}

