package com.tbs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = createRateLimitScript();
    }

    private DefaultRedisScript<Long> createRateLimitScript() {
        String script = 
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "end " +
            "return current";
        
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    public boolean isAllowed(String key, int limit, Duration window) {
        String redisKey = "rate_limit:" + key;
        
        Long count = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(redisKey),
            String.valueOf(window.toSeconds())
        );
        
        if (count == null) {
            log.warn("Failed to increment rate limit counter for key: {}", redisKey);
            return false;
        }
        
        return count <= limit;
    }

    public long getRemainingRequests(String key, int limit) {
        String redisKey = "rate_limit:" + key;
        String currentCount = redisTemplate.opsForValue().get(redisKey);

        if (currentCount == null) {
            return limit;
        }

        try {
            int count = Integer.parseInt(currentCount);
            return Math.max(0, limit - count);
        } catch (NumberFormatException e) {
            log.warn("Invalid rate limit count for key {}: {}", redisKey, currentCount);
            return limit;
        }
    }

    public Duration getTimeToReset(String key) {
        String redisKey = "rate_limit:" + key;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        if (ttl == null || ttl <= 0) {
            return Duration.ZERO;
        }

        return Duration.ofSeconds(ttl);
    }
}

