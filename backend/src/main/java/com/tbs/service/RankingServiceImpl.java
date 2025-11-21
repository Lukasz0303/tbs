package com.tbs.service;

import com.tbs.dto.ranking.RankingAroundItem;
import com.tbs.dto.ranking.RankingAroundResponse;
import com.tbs.dto.ranking.RankingDetailResponse;
import com.tbs.dto.ranking.RankingItem;
import com.tbs.dto.ranking.RankingListResponse;
import com.tbs.exception.UserNotFoundException;
import com.tbs.exception.UserNotInRankingException;
import com.tbs.model.User;
import com.tbs.repository.RankingRepository;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingServiceImpl.class);
    private static final int RANKING_ITEM_COLUMNS = 7;
    private static final int RANKING_AROUND_ITEM_COLUMNS = 6;
    private static final int MAX_PAGE_SIZE = 100;

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;

    public RankingServiceImpl(RankingRepository rankingRepository, UserRepository userRepository) {
        this.rankingRepository = rankingRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "rankings", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#startRank != null ? #startRank : 'null')", unless = "#result == null")
    public RankingListResponse getRankings(Pageable pageable, Integer startRank) {
        try {
            log.debug("Fetching rankings with pageable: {}, startRank: {}", pageable, startRank);
            return getRankingsInternal(pageable, startRank);
        } catch (DataAccessException e) {
            log.error("Database error while fetching rankings: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while fetching rankings: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to fetch rankings: " + e.getMessage(), e);
        }
    }

    private RankingListResponse getRankingsInternal(Pageable pageable, Integer startRank) {

        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Page size must be between 1 and %d", MAX_PAGE_SIZE)
            );
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        List<RankingItem> items;
        long totalCount = rankingRepository.countAll();
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / pageable.getPageSize()) : 0;

        if (totalCount == 0) {
            return new RankingListResponse(
                    List.of(),
                    0L,
                    0,
                    pageable.getPageSize(),
                    pageable.getPageNumber(),
                    true,
                    true
            );
        }

        if (startRank != null && startRank > 0) {
            if (startRank > totalCount) {
                throw new IllegalArgumentException("Start rank exceeds total number of players");
            }
            items = rankingRepository.findRankingsFromPositionRaw(startRank, pageable.getPageSize())
                    .stream()
                    .map(this::mapToRankingItem)
                    .collect(Collectors.toUnmodifiableList());
            return new RankingListResponse(
                    items,
                    totalCount,
                    totalPages,
                    pageable.getPageSize(),
                    pageable.getPageNumber(),
                    pageable.getPageNumber() == 0,
                    pageable.getPageNumber() >= totalPages - 1
            );
        }
        
        int offset = (int) pageable.getOffset();
        items = rankingRepository.findRankingsRaw(offset, pageable.getPageSize())
                .stream()
                .map(this::mapToRankingItem)
                .collect(Collectors.toUnmodifiableList());

        return new RankingListResponse(
                items,
                totalCount,
                totalPages,
                pageable.getPageSize(),
                pageable.getPageNumber(),
                pageable.getPageNumber() == 0,
                pageable.getPageNumber() >= totalPages - 1
        );
    }

    @Override
    @Cacheable(value = "rankingDetail", key = "#userId", unless = "#result == null")
    public RankingDetailResponse getUserRanking(Long userId) {
        try {
            log.debug("Fetching ranking details for userId: {}", userId);
            return getUserRankingInternal(userId);
        } catch (DataAccessException e) {
            log.error("Database error while fetching user ranking for userId {}: {}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while fetching user ranking for userId {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Failed to fetch user ranking: " + e.getMessage(), e);
        }
    }

    private RankingDetailResponse getUserRankingInternal(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.warn("Attempt to get ranking for guest user: {}", userId);
            throw new UserNotInRankingException("Guest users are not included in rankings");
        }

        List<Object[]> results = rankingRepository.findByUserIdRaw(userId);
        if (results.isEmpty()) {
            log.warn("Ranking not found for user: {}", userId);
            throw new UserNotInRankingException("Ranking not found for user with id: " + userId);
        }
        if (results.size() > 1) {
            log.error("Multiple rankings found for user: {} (expected 1, got {})", userId, results.size());
            throw new IllegalStateException("Data integrity violation: multiple rankings for user " + userId);
        }

        Object[] result = results.get(0);
        return mapToRankingDetailResponse(result);
    }

    @Override
    @Cacheable(value = "rankingsAround", key = "#userId + '_' + #range", unless = "#result == null")
    public RankingAroundResponse getRankingsAround(Long userId, Integer range) {
        try {
            log.debug("Fetching rankings around userId: {} with range: {}", userId, range);
            return getRankingsAroundInternal(userId, range);
        } catch (DataAccessException e) {
            log.error("Database error while fetching rankings around userId {}: {}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while fetching rankings around userId {}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("Failed to fetch rankings around user: " + e.getMessage(), e);
        }
    }

    private RankingAroundResponse getRankingsAroundInternal(Long userId, Integer range) {

        if (range == null) {
            range = 5;
        }
        if (range < 1 || range > 10) {
            throw new IllegalArgumentException("Range must be between 1 and 10");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.warn("Attempt to get rankings around guest user: {}", userId);
            throw new UserNotInRankingException("Guest users are not included in rankings");
        }

        List<Object[]> userRanking = rankingRepository.findByUserIdRaw(userId);
        if (userRanking.isEmpty()) {
            throw new UserNotInRankingException("User is not in ranking");
        }

        List<Object[]> results = rankingRepository.findRankingsAroundUserRaw(userId, range);
        
        List<RankingAroundItem> items = results.stream()
                .map(this::mapToRankingAroundItem)
                .collect(Collectors.toUnmodifiableList());

        if (items.isEmpty()) {
            log.debug("No players found around user for userId: {} with range: {}", userId, range);
            return new RankingAroundResponse(items);
        }
        
        log.debug("Found {} players around user for userId: {} with range: {}", items.size(), userId, range);

        return new RankingAroundResponse(items);
    }

    private RankingItem mapToRankingItem(Object[] row) {
        validateRow(row, RANKING_ITEM_COLUMNS, "RankingItem");
        try {
            Long rankPosition = getLongValueOrNull(row[0], "rankPosition");
            if (rankPosition == null) {
                log.error("Rank position is null in ranking item. Row: {}", Arrays.toString(row));
                throw new IllegalStateException("Rank position cannot be null");
            }
            Long userId = getLongValueOrNull(row[1], "userId");
            if (userId == null) {
                log.error("User ID is null in ranking item. Row: {}", Arrays.toString(row));
                throw new IllegalStateException("User ID cannot be null");
            }
            return new RankingItem(
                    rankPosition,
                    userId,
                    getStringValue(row[2], "username"),
                    getLongValue(row[3], "totalPoints"),
                    getIntValue(row[4], "gamesPlayed"),
                    getIntValue(row[5], "gamesWon"),
                    mapTimestampToInstant(row[6])
            );
        } catch (IllegalStateException e) {
            log.error("Failed to map ranking item. Row data: {}", Arrays.toString(row), e);
            throw e;
        }
    }

    private RankingDetailResponse mapToRankingDetailResponse(Object[] row) {
        validateRow(row, RANKING_ITEM_COLUMNS, "RankingDetailResponse");
        try {
            return new RankingDetailResponse(
                    getLongValue(row[0], "rankPosition"),
                    getLongValue(row[1], "userId"),
                    getStringValue(row[2], "username"),
                    getLongValue(row[3], "totalPoints"),
                    getIntValue(row[4], "gamesPlayed"),
                    getIntValue(row[5], "gamesWon"),
                    mapTimestampToInstant(row[6])
            );
        } catch (IllegalStateException e) {
            log.error("Failed to map ranking detail response. Row data: {}", Arrays.toString(row), e);
            throw e;
        }
    }

    private RankingAroundItem mapToRankingAroundItem(Object[] row) {
        validateRow(row, RANKING_AROUND_ITEM_COLUMNS, "RankingAroundItem");
        try {
            return new RankingAroundItem(
                    getLongValue(row[0], "rankPosition"),
                    getLongValue(row[1], "userId"),
                    getStringValue(row[2], "username"),
                    getLongValue(row[3], "totalPoints"),
                    getIntValue(row[4], "gamesPlayed"),
                    getIntValue(row[5], "gamesWon")
            );
        } catch (IllegalStateException e) {
            log.error("Failed to map ranking around item. Row data: {}", Arrays.toString(row), e);
            throw e;
        }
    }

    private void validateRow(Object[] row, int expectedLength, String context) {
        if (row == null) {
            throw new IllegalStateException("Row data cannot be null for " + context);
        }
        if (row.length < expectedLength) {
            throw new IllegalStateException(
                    String.format("Invalid row data for %s: expected %d columns, got %d", context, expectedLength, row.length)
            );
        }
    }

    private long getLongValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Field " + fieldName + " cannot be null");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalStateException("Field " + fieldName + " must be a number, got: " + value.getClass().getName());
    }

    private Long getLongValueOrNull(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalStateException("Field " + fieldName + " must be a number, got: " + value.getClass().getName());
    }

    private int getIntValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Field " + fieldName + " cannot be null");
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalStateException("Field " + fieldName + " must be a number, got: " + value.getClass().getName());
    }

    private String getStringValue(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Field " + fieldName + " cannot be null");
        }
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("Field " + fieldName + " must be a string, got: " + value.getClass().getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"rankings", "rankingDetail", "rankingsAround"}, allEntries = true)
    public void refreshPlayerRankings() {
        try {
            log.debug("Refreshing player_rankings materialized view");
            rankingRepository.refreshPlayerRankings();
            log.info("Successfully refreshed player_rankings materialized view");
        } catch (DataAccessException e) {
            log.error("Database error while refreshing player_rankings materialized view: {}", e.getMessage(), e);
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof org.postgresql.util.PSQLException) {
                    org.postgresql.util.PSQLException psqlEx = (org.postgresql.util.PSQLException) cause;
                    String errorMessage = psqlEx.getServerErrorMessage() != null 
                            ? psqlEx.getServerErrorMessage().getMessage() 
                            : psqlEx.getMessage();
                    log.error("PostgreSQL error: {}", errorMessage);
                    throw new IllegalStateException("Failed to refresh player_rankings materialized view: " + errorMessage, e);
                }
                cause = cause.getCause();
            }
            throw new IllegalStateException("Failed to refresh player_rankings materialized view: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof org.springframework.data.redis.RedisConnectionFailureException ||
                e.getCause() instanceof org.springframework.data.redis.RedisConnectionFailureException ||
                (e.getCause() != null && e.getCause().getClass().getName().contains("redis"))) {
                log.warn("Redis unavailable during cache evict, continuing without cache: {}", e.getMessage());
                return;
            }
            log.error("Unexpected error while refreshing player_rankings materialized view: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to refresh player_rankings materialized view: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"rankings", "rankingDetail", "rankingsAround"}, allEntries = true)
    public void clearRankingsCache() {
        try {
            log.info("Clearing all rankings cache");
        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
            log.warn("Redis unavailable during cache evict, continuing without cache: {}", e.getMessage());
        } catch (Exception e) {
            if (e.getCause() instanceof org.springframework.data.redis.RedisConnectionFailureException ||
                e.getCause() != null && e.getCause().getClass().getName().contains("redis")) {
                log.warn("Redis unavailable during cache evict, continuing without cache: {}", e.getMessage());
                return;
            }
            
            log.warn("Error clearing cache (non-critical): {}", e.getMessage());
        }
    }

    private Instant mapTimestampToInstant(Object timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toInstant();
        }
        if (timestamp instanceof Instant) {
            return (Instant) timestamp;
        }
        throw new IllegalStateException("Unexpected timestamp type: " + timestamp.getClass().getName());
    }
}
