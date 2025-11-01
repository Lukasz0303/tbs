package com.tbs.dto.websocket;

public enum WebSocketMessageType {
    MOVE("MOVE"),
    SURRENDER("SURRENDER"),
    PING("PING"),
    PONG("PONG"),
    MOVE_ACCEPTED("MOVE_ACCEPTED"),
    MOVE_REJECTED("MOVE_REJECTED"),
    OPPONENT_MOVE("OPPONENT_MOVE"),
    GAME_UPDATE("GAME_UPDATE"),
    TIMER_UPDATE("TIMER_UPDATE"),
    GAME_ENDED("GAME_ENDED");

    private final String value;

    WebSocketMessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WebSocketMessageType fromValue(String value) {
        for (WebSocketMessageType type : WebSocketMessageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown WebSocket message type: " + value);
    }
}

