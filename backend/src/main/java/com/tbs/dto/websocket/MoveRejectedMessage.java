package com.tbs.dto.websocket;

public record MoveRejectedMessage(
        WebSocketMessageType type,
        MoveRejectedPayload payload
) implements BaseWebSocketMessage {
    public record MoveRejectedPayload(
            String reason,
            String code
    ) {}

    public MoveRejectedMessage(MoveRejectedPayload payload) {
        this(WebSocketMessageType.MOVE_REJECTED, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

