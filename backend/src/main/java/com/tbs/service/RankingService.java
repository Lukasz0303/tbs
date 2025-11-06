package com.tbs.service;

import com.tbs.dto.ranking.RankingAroundResponse;
import com.tbs.dto.ranking.RankingDetailResponse;
import com.tbs.dto.ranking.RankingListResponse;
import org.springframework.data.domain.Pageable;

public interface RankingService {
    RankingListResponse getRankings(Pageable pageable, Integer startRank);

    RankingDetailResponse getUserRanking(Long userId);

    RankingAroundResponse getRankingsAround(Long userId, Integer range);

    void refreshPlayerRankings();

    void clearRankingsCache();
}
