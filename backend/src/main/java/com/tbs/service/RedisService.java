package com.tbs.service;

import com.tbs.enums.BoardSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private static final String QUEUE_PREFIX = "matchmaking:queue:";
    private static final String USER_PREFIX = "matchmaking:user:";
    private static final String LOCK_PREFIX = "matchmaking:lock:";
    private static final int QUEUE_TTL_SECONDS = 300;
    private static final int LOCK_TTL_SECONDS = 5;

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToQueue(Long userId, BoardSize boardSize) {
        String queueKey = QUEUE_PREFIX + boardSize.name();
        String userKey = USER_PREFIX + userId;
        long timestamp = Instant.now().toEpochMilli();

        redisTemplate.opsForZSet().add(queueKey, userId.toString(), timestamp);
        redisTemplate.opsForValue().set(userKey, boardSize.name(), java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS));
        redisTemplate.expire(queueKey, java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS));

        log.debug("Added user {} to queue for board size {}", userId, boardSize);
    }

    public boolean addToQueueIfNotExists(Long userId, BoardSize boardSize) {
        try {
            String queueKey = QUEUE_PREFIX + boardSize.name();
            String userKey = USER_PREFIX + userId;
            
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                return false;
            }
            
            long timestamp = Instant.now().toEpochMilli();
            
            Boolean addedToZSet = redisTemplate.opsForZSet().addIfAbsent(queueKey, userId.toString(), timestamp);
            if (Boolean.FALSE.equals(addedToZSet)) {
                return false;
            }
            
            Boolean setUserKey = redisTemplate.opsForValue().setIfAbsent(
                    userKey, 
                    boardSize.name(), 
                    java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS)
            );
            
            if (Boolean.FALSE.equals(setUserKey)) {
                redisTemplate.opsForZSet().remove(queueKey, userId.toString());
                return false;
            }
            
            redisTemplate.expire(queueKey, java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS));
            
            log.debug("Added user {} to queue for board size {} (atomic operation)", userId, boardSize);
            return true;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue", e);
        }
    }

    public boolean removeFromQueue(Long userId) {
        try {
            String userKey = USER_PREFIX + userId;
            String boardSizeValue = redisTemplate.opsForValue().get(userKey);

            if (boardSizeValue == null) {
                return false;
            }

            try {
                BoardSize boardSize = BoardSize.valueOf(boardSizeValue);
                String queueKey = QUEUE_PREFIX + boardSize.name();

                redisTemplate.opsForZSet().remove(queueKey, userId.toString());
                redisTemplate.delete(userKey);

                log.debug("Removed user {} from queue for board size {}", userId, boardSize);
                return true;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid board size value in Redis for user {}: {}", userId, boardSizeValue);
                redisTemplate.delete(userKey);
                return false;
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while removing user {} from queue", userId, e);
            throw new IllegalStateException("Failed to remove user from queue due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while removing user {} from queue", userId, e);
            throw new IllegalStateException("Failed to remove user from queue", e);
        }
    }

    public boolean isUserInQueue(Long userId) {
        String userKey = USER_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
    }

    public List<Long> getQueueForBoardSize(BoardSize boardSize) {
        String queueKey = QUEUE_PREFIX + boardSize.name();
        Set<String> userIds = redisTemplate.opsForZSet().range(queueKey, 0, -1);

        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return userIds.stream()
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            log.error("Invalid userId format in Redis queue for board size {}: {}", boardSize, userIds, e);
            return new ArrayList<>();
        }
    }

    public BoardSize getUserBoardSize(Long userId) {
        String userKey = USER_PREFIX + userId;
        String boardSizeValue = redisTemplate.opsForValue().get(userKey);

        if (boardSizeValue == null) {
            return null;
        }

        try {
            return BoardSize.valueOf(boardSizeValue);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid board size value in Redis for user {}: {}", userId, boardSizeValue);
            return null;
        }
    }

    public int getQueueSize(BoardSize boardSize) {
        String queueKey = QUEUE_PREFIX + boardSize.name();
        Long size = redisTemplate.opsForZSet().zCard(queueKey);
        return size != null ? size.intValue() : 0;
    }

    public List<QueueEntry> getAllQueueEntries() {
        List<QueueEntry> entries = new ArrayList<>();
        
        for (BoardSize boardSize : BoardSize.values()) {
            String queueKey = QUEUE_PREFIX + boardSize.name();
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, -1);
            
            if (tuples != null) {
                for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                    if (tuple.getValue() != null && tuple.getScore() != null) {
                        Long userId = Long.parseLong(tuple.getValue());
                        Instant joinedAt = Instant.ofEpochMilli(tuple.getScore().longValue());
                        entries.add(new QueueEntry(userId, boardSize, joinedAt));
                    }
                }
            }
        }
        
        return entries;
    }

    public List<QueueEntry> getQueueEntriesForBoardSize(BoardSize boardSize) {
        List<QueueEntry> entries = new ArrayList<>();
        String queueKey = QUEUE_PREFIX + boardSize.name();
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, -1);
        
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() != null && tuple.getScore() != null) {
                    Long userId = Long.parseLong(tuple.getValue());
                    Instant joinedAt = Instant.ofEpochMilli(tuple.getScore().longValue());
                    entries.add(new QueueEntry(userId, boardSize, joinedAt));
                }
            }
        }
        
        return entries;
    }

    public Map<Long, Instant> getJoinedAtTimestamps(List<Long> userIds) {
        Map<Long, Instant> timestamps = new HashMap<>();
        
        for (BoardSize boardSize : BoardSize.values()) {
            String queueKey = QUEUE_PREFIX + boardSize.name();
            for (Long userId : userIds) {
                Double score = redisTemplate.opsForZSet().score(queueKey, userId.toString());
                if (score != null) {
                    timestamps.put(userId, Instant.ofEpochMilli(score.longValue()));
                }
            }
        }
        
        return timestamps;
    }

    public boolean acquireLock(String lockKey, int timeoutSeconds) {
        try {
            String fullLockKey = LOCK_PREFIX + lockKey;
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    fullLockKey, 
                    "locked", 
                    java.time.Duration.ofSeconds(timeoutSeconds)
            );
            return Boolean.TRUE.equals(acquired);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while acquiring lock: {}", lockKey, e);
            throw new IllegalStateException("Failed to acquire lock due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while acquiring lock: {}", lockKey, e);
            throw new IllegalStateException("Failed to acquire lock", e);
        }
    }

    public void releaseLock(String lockKey) {
        try {
            String fullLockKey = LOCK_PREFIX + lockKey;
            redisTemplate.delete(fullLockKey);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failure while releasing lock: {} (non-critical)", lockKey, e);
        } catch (Exception e) {
            log.warn("Unexpected error while releasing lock: {} (non-critical)", lockKey, e);
        }
    }

    public record QueueEntry(Long userId, BoardSize boardSize, Instant joinedAt) {}
}

