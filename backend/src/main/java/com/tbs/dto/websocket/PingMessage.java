package com.tbs.dto.websocket;

import java.time.Instant;

public record PingMessage(
        WebSocketMessageType type,
        PingPayload payload
) implements BaseWebSocketMessage {
    public record PingPayload(Instant timestamp) {}

    public PingMessage(PingPayload payload) {
        this(WebSocketMessageType.PING, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

