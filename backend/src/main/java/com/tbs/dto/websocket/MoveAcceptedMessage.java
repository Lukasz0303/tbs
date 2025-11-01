package com.tbs.dto.websocket;

import com.tbs.dto.common.BoardState;
import com.tbs.enums.PlayerSymbol;
import java.time.Instant;

public record MoveAcceptedMessage(
        WebSocketMessageType type,
        MoveAcceptedPayload payload
) implements BaseWebSocketMessage {
    public record MoveAcceptedPayload(
            long moveId,
            int row,
            int col,
            PlayerSymbol playerSymbol,
            BoardState boardState,
            PlayerSymbol currentPlayerSymbol,
            Instant nextMoveAt
    ) {}

    public MoveAcceptedMessage(MoveAcceptedPayload payload) {
        this(WebSocketMessageType.MOVE_ACCEPTED, payload);
    }

    @Override
    public WebSocketMessageType type() {
        return type;
    }
}

