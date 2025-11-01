package com.tbs.dto.websocket;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.GameStatus;

public record GameUpdateMessage(
        WebSocketMessageType type,
        GameUpdatePayload payload
) implements BaseWebSocketMessage {
    public record GameUpdatePayload(
            long gameId,
            GameStatus status,
            WinnerInfo winner,
            BoardState boardState
    ) {}

    public GameUpdateMessage(GameUpdatePayload payload) {
        this(WebSocketMessageType.GAME_UPDATE, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

