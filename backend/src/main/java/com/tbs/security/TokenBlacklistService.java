package com.tbs.security;

import com.tbs.exception.TokenBlacklistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlacklist(String tokenId, Date expirationTime) {
        if (tokenId == null || expirationTime == null) {
            log.warn("Attempted to blacklist token with null ID or expiration");
            return;
        }

        try {
            long ttlMillis = expirationTime.getTime() - System.currentTimeMillis();
            
            if (ttlMillis <= 0) {
                log.debug("Token already expired, skipping blacklist: tokenId={}", tokenId);
                return;
            }

            String key = BLACKLIST_PREFIX + tokenId;
            redisTemplate.opsForValue().set(key, "true", Duration.ofMillis(ttlMillis));
            
            log.debug("Token added to blacklist: tokenId={}, ttl={}ms", tokenId, ttlMillis);
        } catch (Exception e) {
            log.error("Failed to add token to blacklist: tokenId={}", tokenId, e);
            throw new TokenBlacklistException("Failed to blacklist token", e);
        }
    }

    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }

        try {
            String key = BLACKLIST_PREFIX + tokenId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token blacklist: tokenId={}", tokenId, e);
            return false;
        }
    }

    public void removeFromBlacklist(String tokenId) {
        if (tokenId == null) {
            return;
        }

        try {
            String key = BLACKLIST_PREFIX + tokenId;
            redisTemplate.delete(key);
            log.debug("Token removed from blacklist: tokenId={}", tokenId);
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: tokenId={}", tokenId, e);
        }
    }
}

