package com.tbs.dto.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MoveMessage.class, name = "MOVE"),
        @JsonSubTypes.Type(value = SurrenderMessage.class, name = "SURRENDER"),
        @JsonSubTypes.Type(value = PingMessage.class, name = "PING"),
        @JsonSubTypes.Type(value = PongMessage.class, name = "PONG"),
        @JsonSubTypes.Type(value = MoveAcceptedMessage.class, name = "MOVE_ACCEPTED"),
        @JsonSubTypes.Type(value = MoveRejectedMessage.class, name = "MOVE_REJECTED"),
        @JsonSubTypes.Type(value = OpponentMoveMessage.class, name = "OPPONENT_MOVE"),
        @JsonSubTypes.Type(value = GameUpdateMessage.class, name = "GAME_UPDATE"),
        @JsonSubTypes.Type(value = TimerUpdateMessage.class, name = "TIMER_UPDATE"),
        @JsonSubTypes.Type(value = GameEndedMessage.class, name = "GAME_ENDED")
})
public sealed interface BaseWebSocketMessage
        permits MoveMessage, SurrenderMessage, PingMessage, PongMessage,
                MoveAcceptedMessage, MoveRejectedMessage, OpponentMoveMessage,
                GameUpdateMessage, TimerUpdateMessage, GameEndedMessage {
    WebSocketMessageType type();
}

