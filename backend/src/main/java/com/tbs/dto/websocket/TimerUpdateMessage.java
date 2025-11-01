package com.tbs.dto.websocket;

import com.tbs.enums.PlayerSymbol;

public record TimerUpdateMessage(
        WebSocketMessageType type,
        TimerUpdatePayload payload
) implements BaseWebSocketMessage {
    public record TimerUpdatePayload(
            int remainingSeconds,
            PlayerSymbol currentPlayerSymbol
    ) {}

    public TimerUpdateMessage(TimerUpdatePayload payload) {
        this(WebSocketMessageType.TIMER_UPDATE, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

