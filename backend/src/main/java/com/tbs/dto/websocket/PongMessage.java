package com.tbs.dto.websocket;

import java.time.Instant;

public record PongMessage(
        WebSocketMessageType type,
        PongPayload payload
) implements BaseWebSocketMessage {
    public record PongPayload(Instant timestamp) {}

    public PongMessage(PongPayload payload) {
        this(WebSocketMessageType.PONG, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

