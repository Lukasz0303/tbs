package com.tbs.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final Map<Long, Map<Long, String>> gameSessions = new ConcurrentHashMap<>();

    public void addSession(Long gameId, Long userId, String sessionId) {
        gameSessions.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(userId, sessionId);
        log.debug("Added WebSocket session: gameId={}, userId={}, sessionId={}", gameId, userId, sessionId);
    }

    public void removeSession(Long gameId, Long userId) {
        Map<Long, String> gameSessionMap = gameSessions.get(gameId);
        if (gameSessionMap != null) {
            gameSessionMap.remove(userId);
            if (gameSessionMap.isEmpty()) {
                gameSessions.remove(gameId);
            }
            log.debug("Removed WebSocket session: gameId={}, userId={}", gameId, userId);
        }
    }

    public String getSessionId(Long gameId, Long userId) {
        Map<Long, String> gameSessionMap = gameSessions.get(gameId);
        if (gameSessionMap == null) {
            return null;
        }
        return gameSessionMap.get(userId);
    }

    public Map<Long, String> getGameSessions(Long gameId) {
        return gameSessions.getOrDefault(gameId, Map.of());
    }

    public void removeAllGameSessions(Long gameId) {
        gameSessions.remove(gameId);
        log.debug("Removed all WebSocket sessions for game: gameId={}", gameId);
    }
}

