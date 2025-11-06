package com.tbs.repository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface RankingRepository {
    List<Object[]> findRankingsRaw(int offset, int limit);
    List<Object[]> findRankingsFromPositionRaw(int startRank, int size);
    List<Object[]> findByUserIdRaw(Long userId);
    List<Object[]> findRankingsAroundUserRaw(Long userId, int range);
    long countAll();
    void refreshPlayerRankings();
}
