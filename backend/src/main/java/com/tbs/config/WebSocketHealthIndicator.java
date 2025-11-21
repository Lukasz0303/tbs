package com.tbs.config;

import com.tbs.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHealthIndicator.class);
    
    private final WebSocketSessionManager sessionManager;

    public WebSocketHealthIndicator(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Health health() {
        try {
            int activeConnections = sessionManager.getActiveConnectionCount();
            boolean isHealthy = sessionManager.hasActiveConnections() || activeConnections >= 0;

            if (!isHealthy) {
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("reason", "WebSocket handler not properly initialized")
                        .build();
            }
            
            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("activeConnections", activeConnections)
                    .build();
        } catch (Exception e) {
            log.error("Error checking WebSocket health", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

