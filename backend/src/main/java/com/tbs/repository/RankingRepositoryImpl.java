package com.tbs.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class RankingRepositoryImpl implements RankingRepository {

    private static final int MAX_OFFSET = 1_000_000;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findRankingsRaw(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative, got: " + offset);
        }
        if (offset > MAX_OFFSET) {
            throw new IllegalArgumentException("Offset exceeds maximum allowed value: " + MAX_OFFSET);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive, got: " + limit);
        }
        String sql = """
                SELECT pr.rank_position, pr.id, pr.username, pr.total_points,
                       pr.games_played, pr.games_won, pr.created_at
                FROM player_rankings pr
                ORDER BY pr.rank_position
                OFFSET :offset LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("offset", offset);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findRankingsFromPositionRaw(int startRank, int size) {
        if (startRank < 1) {
            throw new IllegalArgumentException("Start rank must be at least 1, got: " + startRank);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        String sql = """
                SELECT pr.rank_position, pr.id, pr.username, pr.total_points,
                       pr.games_played, pr.games_won, pr.created_at
                FROM player_rankings pr
                WHERE pr.rank_position >= :startRank
                ORDER BY pr.rank_position
                LIMIT :size
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startRank", startRank);
        query.setParameter("size", size);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findByUserIdRaw(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive, got: " + userId);
        }
        String sql = """
                SELECT pr.rank_position, pr.id, pr.username, pr.total_points,
                       pr.games_played, pr.games_won, pr.created_at
                FROM player_rankings pr
                WHERE pr.id = :userId
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> findRankingsAroundUserRaw(Long userId, int range) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive, got: " + userId);
        }
        if (range < 1) {
            throw new IllegalArgumentException("Range must be at least 1, got: " + range);
        }
        String sql = """
                WITH user_rank AS (
                    SELECT rank_position
                    FROM player_rankings
                    WHERE id = :userId
                )
                SELECT pr.rank_position, pr.id, pr.username, pr.total_points,
                       pr.games_played, pr.games_won
                FROM player_rankings pr, user_rank ur
                WHERE pr.rank_position BETWEEN GREATEST(1, ur.rank_position - :range) 
                                           AND (ur.rank_position + :range)
                ORDER BY pr.rank_position
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("range", range);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        String sql = "SELECT COUNT(*) FROM player_rankings";
        Query query = entityManager.createNativeQuery(sql);
        Object result = query.getSingleResult();
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return 0L;
    }

    @Override
    @Transactional
    public void refreshPlayerRankings() {
        String sql = "SELECT refresh_player_rankings()";
        Query query = entityManager.createNativeQuery(sql);
        query.getSingleResult();
    }
}
