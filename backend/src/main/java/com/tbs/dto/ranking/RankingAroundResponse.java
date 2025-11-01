package com.tbs.dto.ranking;

import java.util.List;

public record RankingAroundResponse(
        List<RankingAroundItem> items
) {
    public RankingAroundResponse(List<RankingAroundItem> items) {
        this.items = items != null ? items : List.of();
    }
}

