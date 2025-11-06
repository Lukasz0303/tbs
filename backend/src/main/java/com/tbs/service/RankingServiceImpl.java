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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingServiceImpl.class);
    private static final int RANKING_ITEM_COLUMNS = 7;
    private static final int RANKING_AROUND_ITEM_COLUMNS = 6;

    private final RankingRepository rankingRepository;
    private final UserRepository userRepository;

    public RankingServiceImpl(RankingRepository rankingRepository, UserRepository userRepository) {
        this.rankingRepository = rankingRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "rankings", key = "'rankings_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#pageable.sort != null ? #pageable.sort.toString().hashCode() : 0) + '_' + (#startRank != null ? #startRank : 'null')")
    public RankingListResponse getRankings(Pageable pageable, Integer startRank) {
        log.debug("Fetching rankings with pageable: {}, startRank: {}", pageable, startRank);

        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        List<RankingItem> items;
        long totalCount = rankingRepository.countAll();
        int totalPages = (int) Math.ceil((double) totalCount / pageable.getPageSize());

        if (startRank != null && startRank > 0) {
            if (startRank > totalCount) {
                throw new IllegalArgumentException("Start rank exceeds total number of players");
            }
            items = rankingRepository.findRankingsFromPositionRaw(startRank, pageable.getPageSize())
                    .stream()
                    .map(this::mapToRankingItem)
                    .collect(Collectors.toList());
        } else {
            int offset = (int) pageable.getOffset();
            items = rankingRepository.findRankingsRaw(offset, pageable.getPageSize())
                    .stream()
                    .map(this::mapToRankingItem)
                    .collect(Collectors.toList());
        }

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
    @Cacheable(value = "rankingDetail", key = "#userId")
    public RankingDetailResponse getUserRanking(Long userId) {
        log.debug("Fetching ranking details for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.warn("Attempt to get ranking for guest user: {}", userId);
            throw new UserNotInRankingException("Guest users are not included in rankings");
        }

        List<Object[]> results = rankingRepository.findByUserIdRaw(userId);
        if (results.isEmpty()) {
            log.warn("Ranking not found for user: {}", userId);
            throw new UserNotFoundException("Ranking not found for user with id: " + userId);
        }

        Object[] result = results.get(0);
        return mapToRankingDetailResponse(result);
    }

    @Override
    @Cacheable(value = "rankingsAround", key = "#userId + '_' + #range")
    public RankingAroundResponse getRankingsAround(Long userId, Integer range) {
        log.debug("Fetching rankings around userId: {} with range: {}", userId, range);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getIsGuest())) {
            log.warn("Attempt to get rankings around guest user: {}", userId);
            throw new UserNotInRankingException("Guest users are not included in rankings");
        }

        List<Object[]> results = rankingRepository.findRankingsAroundUserRaw(userId, range);
        if (results.isEmpty()) {
            log.warn("Rankings around user not found for userId: {}", userId);
            throw new UserNotFoundException("Ranking not found for user with id: " + userId);
        }

        List<RankingAroundItem> items = results.stream()
                .map(this::mapToRankingAroundItem)
                .collect(Collectors.toList());

        boolean userFound = items.stream()
                .anyMatch(item -> Objects.equals(item.userId(), userId));

        if (!userFound) {
            log.warn("User rank position not found in results for userId: {}", userId);
            throw new UserNotFoundException("Ranking position not found for user with id: " + userId);
        }

        return new RankingAroundResponse(items);
    }

    private RankingItem mapToRankingItem(Object[] row) {
        validateRow(row, RANKING_ITEM_COLUMNS, "RankingItem");
        return new RankingItem(
                getLongValue(row[0], "rankPosition"),
                getLongValue(row[1], "userId"),
                getStringValue(row[2], "username"),
                getLongValue(row[3], "totalPoints"),
                getIntValue(row[4], "gamesPlayed"),
                getIntValue(row[5], "gamesWon"),
                mapTimestampToInstant(row[6])
        );
    }

    private RankingDetailResponse mapToRankingDetailResponse(Object[] row) {
        validateRow(row, RANKING_ITEM_COLUMNS, "RankingDetailResponse");
        return new RankingDetailResponse(
                getLongValue(row[0], "rankPosition"),
                getLongValue(row[1], "userId"),
                getStringValue(row[2], "username"),
                getLongValue(row[3], "totalPoints"),
                getIntValue(row[4], "gamesPlayed"),
                getIntValue(row[5], "gamesWon"),
                mapTimestampToInstant(row[6])
        );
    }

    private RankingAroundItem mapToRankingAroundItem(Object[] row) {
        validateRow(row, RANKING_AROUND_ITEM_COLUMNS, "RankingAroundItem");
        return new RankingAroundItem(
                getLongValue(row[0], "rankPosition"),
                getLongValue(row[1], "userId"),
                getStringValue(row[2], "username"),
                getLongValue(row[3], "totalPoints"),
                getIntValue(row[4], "gamesPlayed"),
                getIntValue(row[5], "gamesWon")
        );
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
        log.debug("Refreshing player_rankings materialized view");
        try {
            rankingRepository.refreshPlayerRankings();
            log.info("Successfully refreshed player_rankings materialized view");
        } catch (Exception e) {
            log.error("Error refreshing player_rankings materialized view", e);
            throw new RuntimeException("Failed to refresh player rankings", e);
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
        log.warn("Unexpected timestamp type: {}", timestamp.getClass());
        return null;
    }
}
