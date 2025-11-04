package com.tbs.service;

import com.tbs.dto.websocket.BaseWebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketMessageStorageService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageStorageService.class);
    private static final long USER_KEY_MULTIPLIER = 1_000_000L;
    private static final long MESSAGE_TTL_MS = 24 * 60 * 60 * 1000L;

    private final Map<Long, Map<Long, List<BaseWebSocketMessage>>> gameMessages = new ConcurrentHashMap<>();
    private final Map<Long, List<BaseWebSocketMessage>> userMessages = new ConcurrentHashMap<>();
    private final Map<BaseWebSocketMessage, Long> messageTimestamps = new ConcurrentHashMap<>();

    public void storeMessage(Long gameId, Long userId, BaseWebSocketMessage message) {
        if (gameId == null || userId == null || message == null) {
            return;
        }

        gameMessages.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> new ArrayList<>())
                .add(message);

        Long userKey = gameId * USER_KEY_MULTIPLIER + userId;
        userMessages.computeIfAbsent(userKey, k -> new ArrayList<>())
                .add(message);

        messageTimestamps.put(message, System.currentTimeMillis());

        log.debug("Stored WebSocket message for gameId={}, userId={}, type={}", 
                gameId, userId, message.type());
    }

    public List<BaseWebSocketMessage> getMessagesForGame(Long gameId) {
        if (gameId == null) {
            return Collections.emptyList();
        }

        Map<Long, List<BaseWebSocketMessage>> gameUsers = gameMessages.get(gameId);
        if (gameUsers == null || gameUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<BaseWebSocketMessage> allMessages = new ArrayList<>();
        gameUsers.values().forEach(allMessages::addAll);
        allMessages.sort(Comparator.comparing(msg -> getMessageTimestamp(msg)));

        log.debug("Retrieved {} messages for gameId={}", allMessages.size(), gameId);
        return allMessages;
    }

    public List<BaseWebSocketMessage> getMessagesForUser(Long gameId, Long userId) {
        if (gameId == null || userId == null) {
            return Collections.emptyList();
        }

        Map<Long, List<BaseWebSocketMessage>> gameUsers = gameMessages.get(gameId);
        if (gameUsers == null) {
            return Collections.emptyList();
        }

        List<BaseWebSocketMessage> messages = gameUsers.get(userId);
        if (messages == null) {
            return Collections.emptyList();
        }

        log.info("Retrieved {} messages for gameId={}, userId={}", messages.size(), gameId, userId);
        return new ArrayList<>(messages);
    }

    public void clearMessagesForGame(Long gameId) {
        if (gameId == null) {
            return;
        }

        Map<Long, List<BaseWebSocketMessage>> gameUsers = gameMessages.remove(gameId);
        if (gameUsers != null) {
            gameUsers.forEach((userId, messages) -> {
                Long userKey = gameId * USER_KEY_MULTIPLIER + userId;
                List<BaseWebSocketMessage> userMsgList = userMessages.remove(userKey);
                if (userMsgList != null) {
                    userMsgList.forEach(messageTimestamps::remove);
                }
            });
        }

        log.debug("Cleared messages for gameId={}", gameId);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanOldMessages() {
        long cutoffTime = System.currentTimeMillis() - MESSAGE_TTL_MS;
        
        List<BaseWebSocketMessage> messagesToRemove = messageTimestamps.entrySet().stream()
                .filter(entry -> entry.getValue() < cutoffTime)
                .map(Map.Entry::getKey)
                .toList();

        messagesToRemove.forEach(msg -> {
            messageTimestamps.remove(msg);
            gameMessages.values().forEach(gameUsers -> 
                gameUsers.values().forEach(messages -> messages.remove(msg))
            );
            userMessages.values().forEach(messages -> messages.remove(msg));
        });

        gameMessages.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        gameMessages.values().forEach(gameUsers -> 
            gameUsers.entrySet().removeIf(entry -> entry.getValue().isEmpty())
        );
        userMessages.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        log.debug("Cleaned {} old messages. Remaining messages: {}", messagesToRemove.size(), messageTimestamps.size());
    }

    private long getMessageTimestamp(BaseWebSocketMessage message) {
        if (message == null) {
            return 0L;
        }
        return messageTimestamps.getOrDefault(message, System.currentTimeMillis());
    }
}

