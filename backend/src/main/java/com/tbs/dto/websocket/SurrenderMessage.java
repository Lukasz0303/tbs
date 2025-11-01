package com.tbs.dto.websocket;

public record SurrenderMessage(
        WebSocketMessageType type,
        EmptyPayload payload
) implements BaseWebSocketMessage {
    public record EmptyPayload() {}

    public SurrenderMessage() {
        this(WebSocketMessageType.SURRENDER, new EmptyPayload());
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

