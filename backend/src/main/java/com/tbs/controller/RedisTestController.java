package com.tbs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/redis")
@Tag(name = "Redis Test", description = "Test endpoints for Redis connectivity (development only)")
@ConditionalOnProperty(name = "app.test-endpoints.enabled", havingValue = "true", matchIfMissing = false)
public class RedisTestController {

    private static final Logger log = LoggerFactory.getLogger(RedisTestController.class);
    private final RedisTemplate<String, String> redisTemplate;

    public RedisTestController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/write")
    @Operation(summary = "Write to Redis", description = "Writes a test value to Redis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value written successfully"),
            @ApiResponse(responseCode = "503", description = "Redis connection failure"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    public ResponseEntity<Map<String, Object>> writeToRedis(
            @Parameter(description = "Redis key", example = "test-key")
            @RequestParam(required = false, defaultValue = "test-key") String key,
            @Parameter(description = "Redis value", example = "test-value")
            @RequestParam(required = false, defaultValue = "test-value") String value
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String timestamp = Instant.now().toString();
            String fullValue = value + " - " + timestamp;
            
            redisTemplate.opsForValue().set(key, fullValue);
            
            response.put("success", true);
            response.put("key", key != null ? key : "null");
            response.put("value", fullValue != null ? fullValue : "null");
            response.put("message", "Value successfully written to Redis");
            response.put("timestamp", timestamp);
            
            log.info("Successfully wrote to Redis - key: {}, value: {}", key, fullValue);
            
            return ResponseEntity.ok(response);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while writing", e);
            response.put("success", false);
            response.put("error", "Redis connection failure");
            response.put("message", e.getMessage());
            return ResponseEntity.status(503).body(response);
        } catch (Exception e) {
            log.error("Unexpected error while writing to Redis", e);
            response.put("success", false);
            response.put("error", "Unexpected error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/read")
    @Operation(summary = "Read from Redis", description = "Reads a test value from Redis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value read successfully"),
            @ApiResponse(responseCode = "503", description = "Redis connection failure"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    public ResponseEntity<Map<String, Object>> readFromRedis(
            @Parameter(description = "Redis key", example = "test-key")
            @RequestParam(required = false, defaultValue = "test-key") String key
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            
            response.put("success", true);
            response.put("key", key != null ? key : "null");
            response.put("value", value != null ? value : "null");
            response.put("exists", value != null);
            response.put("message", value != null ? "Value found in Redis" : "Key not found in Redis");
            
            log.info("Read from Redis - key: {}, value: {}, exists: {}", key, value, value != null);
            
            return ResponseEntity.ok(response);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure while reading", e);
            response.put("success", false);
            response.put("error", "Redis connection failure");
            response.put("message", e.getMessage());
            return ResponseEntity.status(503).body(response);
        } catch (Exception e) {
            log.error("Unexpected error while reading from Redis", e);
            response.put("success", false);
            response.put("error", "Unexpected error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/ping")
    @Operation(summary = "Ping Redis", description = "Tests Redis connectivity by writing and reading a test value")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis ping successful"),
            @ApiResponse(responseCode = "503", description = "Redis connection failure"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    public ResponseEntity<Map<String, Object>> pingRedis() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String testKey = "ping-test-" + Instant.now().toEpochMilli();
            String testValue = "pong";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            boolean success = testValue.equals(retrievedValue);
            
            response.put("success", success);
            response.put("message", success ? "Redis is working correctly" : "Redis returned unexpected value");
            response.put("timestamp", Instant.now().toString());
            
            log.info("Redis ping test - success: {}", success);
            
            return ResponseEntity.ok(response);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failure during ping", e);
            response.put("success", false);
            response.put("error", "Redis connection failure");
            response.put("message", e.getMessage());
            return ResponseEntity.status(503).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during Redis ping", e);
            response.put("success", false);
            response.put("error", "Unexpected error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Get Redis info", description = "Retrieves Redis connection information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis info retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    public ResponseEntity<Map<String, Object>> getRedisInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                response.put("success", false);
                response.put("error", "Connection factory is null");
                return ResponseEntity.status(500).body(response);
            }
            
            var connection = connectionFactory.getConnection();
            String connectionInfo = connection != null ? connection.toString() : "null";
            
            response.put("success", true);
            response.put("connectionFactory", connectionFactory.getClass().getSimpleName());
            response.put("connectionInfo", connectionInfo);
            response.put("timestamp", Instant.now().toString());
            
            log.info("Redis info retrieved - connectionFactory: {}", connectionFactory.getClass().getSimpleName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting Redis info", e);
            response.put("success", false);
            response.put("error", "Failed to get Redis info");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

