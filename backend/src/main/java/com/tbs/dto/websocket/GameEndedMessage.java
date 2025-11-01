package com.tbs.dto.websocket;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.GameStatus;

public record GameEndedMessage(
        WebSocketMessageType type,
        GameEndedPayload payload
) implements BaseWebSocketMessage {
    public record GameEndedPayload(
            long gameId,
            GameStatus status,
            WinnerInfo winner,
            BoardState finalBoardState,
            int totalMoves
    ) {}

    public GameEndedMessage(GameEndedPayload payload) {
        this(WebSocketMessageType.GAME_ENDED, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

