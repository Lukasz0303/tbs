package com.tbs.dto.websocket;

import com.tbs.enums.PlayerSymbol;

public record MoveMessage(
        WebSocketMessageType type,
        MovePayload payload
) implements BaseWebSocketMessage {
    public record MovePayload(
            int row,
            int col,
            PlayerSymbol playerSymbol
    ) {}

    public MoveMessage(MovePayload payload) {
        this(WebSocketMessageType.MOVE, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

