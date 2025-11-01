package com.tbs.dto.websocket;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.PlayerSymbol;
import java.time.Instant;

public record OpponentMoveMessage(
        WebSocketMessageType type,
        OpponentMovePayload payload
) implements BaseWebSocketMessage {
    public record OpponentMovePayload(
            int row,
            int col,
            PlayerSymbol playerSymbol,
            BoardState boardState,
            PlayerSymbol currentPlayerSymbol,
            Instant nextMoveAt
    ) {}

    public OpponentMoveMessage(OpponentMovePayload payload) {
        this(WebSocketMessageType.OPPONENT_MOVE, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

