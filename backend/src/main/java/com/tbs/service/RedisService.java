package com.tbs.service;

import com.tbs.enums.BoardSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import io.lettuce.core.RedisCommandTimeoutException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private static final String QUEUE_PREFIX = "matchmaking:queue:";
    private static final String USER_PREFIX = "matchmaking:user:";
    private static final String LOCK_PREFIX = "matchmaking:lock:";
    private static final String ACTIVE_GAME_PREFIX = "active_game:";
    private static final int QUEUE_TTL_SECONDS = 300;
    private static final int LOCK_TTL_SECONDS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> checkAndAddToQueueScript;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.checkAndAddToQueueScript = createCheckAndAddToQueueScript();
    }

    private DefaultRedisScript<Long> createCheckAndAddToQueueScript() {
        String script =
            "local activeGameKey = KEYS[1] " +
            "local queueKey = KEYS[2] " +
            "local userKey = KEYS[3] " +
            "local userId = ARGV[1] " +
            "local boardSize = ARGV[2] " +
            "local timestamp = ARGV[3] " +
            "local queueTtl = ARGV[4] " +
            "if redis.call('EXISTS', activeGameKey) == 0 then " +
            "    local addedToZSet = redis.call('ZADD', queueKey, 'NX', timestamp, userId) " +
            "    if addedToZSet == 1 then " +
            "        redis.call('SET', userKey, boardSize, 'EX', queueTtl) " +
            "        redis.call('EXPIRE', queueKey, queueTtl) " +
            "        return 1 " +
            "    end " +
            "    return 0 " +
            "end " +
            "return 0";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    public void addToQueue(Long userId, BoardSize boardSize) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(boardSize, "BoardSize cannot be null");
        
        String queueKey = QUEUE_PREFIX + boardSize.name();
        String userKey = USER_PREFIX + userId;
        long timestamp = Instant.now().toEpochMilli();

        redisTemplate.opsForZSet().add(queueKey, userId.toString(), timestamp);
        redisTemplate.opsForValue().set(userKey, boardSize.name(), java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS));
        redisTemplate.expire(queueKey, java.time.Duration.ofSeconds(QUEUE_TTL_SECONDS));

        log.debug("Added user {} to queue for board size {}", userId, boardSize);
    }

    public boolean addToQueueIfNotExists(Long userId, BoardSize boardSize) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(boardSize, "BoardSize cannot be null");
        
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
        } catch (RedisCommandTimeoutException e) {
            log.error("Redis operation timeout while adding user {} to queue", userId, e);
            throw new IllegalStateException("Redis operation timed out", e);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue", e);
        }
    }

    public boolean removeFromQueue(Long userId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        
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
        } catch (RedisCommandTimeoutException e) {
            log.error("Redis operation timeout while removing user {} from queue", userId, e);
            throw new IllegalStateException("Redis operation timed out", e);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while removing user {} from queue", userId, e);
            throw new IllegalStateException("Failed to remove user from queue due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while removing user {} from queue", userId, e);
            throw new IllegalStateException("Failed to remove user from queue", e);
        }
    }

    public boolean isUserInQueue(Long userId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        
        String userKey = USER_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
    }

    public List<Long> getQueueForBoardSize(BoardSize boardSize) {
        Objects.requireNonNull(boardSize, "BoardSize cannot be null");
        
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
        Objects.requireNonNull(userId, "UserId cannot be null");
        
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
        Objects.requireNonNull(boardSize, "BoardSize cannot be null");
        
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

    public boolean addToQueueIfNotActive(Long userId, BoardSize boardSize) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(boardSize, "BoardSize cannot be null");
        
        try {
            String activeGameKey = ACTIVE_GAME_PREFIX + userId;
            String queueKey = QUEUE_PREFIX + boardSize.name();
            String userKey = USER_PREFIX + userId;
            long timestamp = Instant.now().toEpochMilli();
            String queueTtl = String.valueOf(QUEUE_TTL_SECONDS);

            Long result = redisTemplate.execute(
                    checkAndAddToQueueScript,
                    Arrays.asList(activeGameKey, queueKey, userKey),
                    userId.toString(),
                    boardSize.name(),
                    String.valueOf(timestamp),
                    queueTtl
            );

            boolean added = result != null && result == 1;
            if (added) {
                log.debug("Atomically added user {} to queue for board size {} (no active game)", userId, boardSize);
            } else {
                log.debug("User {} not added to queue - has active game or already in queue", userId);
            }
            return added;
        } catch (RedisCommandTimeoutException e) {
            log.error("Redis operation timeout while atomically adding user {} to queue", userId, e);
            throw new IllegalStateException("Redis operation timed out", e);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while atomically adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue due to Redis connection failure", e);
        } catch (Exception e) {
            log.error("Unexpected error while atomically adding user {} to queue", userId, e);
            throw new IllegalStateException("Failed to add user to queue", e);
        }
    }

    public record QueueEntry(Long userId, BoardSize boardSize, Instant joinedAt) {}
}

