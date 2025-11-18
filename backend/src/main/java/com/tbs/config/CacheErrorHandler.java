package com.tbs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class CacheErrorHandler implements org.springframework.cache.interceptor.CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheErrorHandler.class);

    private boolean isRedisError(RuntimeException exception) {
        if (exception instanceof RedisConnectionFailureException) {
            return true;
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof RedisConnectionFailureException ||
                cause.getClass().getName().contains("redis") ||
                cause.getClass().getName().contains("Redis")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        if (isRedisError(exception)) {
            log.debug("Redis unavailable for cache get '{}' key '{}', continuing without cache: {}", 
                cache.getName(), key, exception.getMessage());
        } else {
            log.warn("Cache get error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
        }
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, @Nullable Object value) {
        if (isRedisError(exception)) {
            log.debug("Redis unavailable for cache put '{}' key '{}', continuing without cache: {}", 
                cache.getName(), key, exception.getMessage());
        } else {
            log.warn("Cache put error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
        }
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        if (isRedisError(exception)) {
            log.debug("Redis unavailable for cache evict '{}' key '{}', continuing without cache: {}", 
                cache.getName(), key, exception.getMessage());
        } else {
            log.warn("Cache evict error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
        }
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
        if (isRedisError(exception)) {
            log.debug("Redis unavailable for cache clear '{}', continuing without cache: {}", 
                cache.getName(), exception.getMessage());
        } else {
            log.warn("Cache clear error for cache '{}': {}", cache.getName(), exception.getMessage());
        }
    }
}

