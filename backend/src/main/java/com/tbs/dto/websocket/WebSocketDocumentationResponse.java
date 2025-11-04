package com.tbs.dto.websocket;

import java.util.List;

public record WebSocketDocumentationResponse(
        String endpoint,
        String protocol,
        String url,
        String subprotocol,
        ConnectionInfo connection,
        AuthenticationInfo authentication,
        RequirementsInfo requirements,
        List<MessageTypeInfo> clientToServerMessages,
        List<MessageTypeInfo> serverToClientMessages,
        FlowInfo flow,
        ErrorHandlingInfo errorHandling,
        String detailedDocumentation
) {
    public record ConnectionInfo(
            String url,
            String protocol,
            String subprotocol,
            String example
    ) {}

    public record AuthenticationInfo(
            String method,
            String header,
            String description
    ) {}

    public record RequirementsInfo(
            boolean mustBeParticipant,
            String gameType,
            String gameStatus,
            String description
    ) {}

    public record MessageTypeInfo(
            String type,
            String description,
            String example,
            List<String> validation,
            List<String> businessRules
    ) {}

    public record FlowInfo(
            String handshake,
            String move,
            String surrender,
            String timer,
            String keepAlive,
            String disconnection
    ) {}

    public record ErrorHandlingInfo(
            List<String> errorCodes,
            String reconnectionWindow,
            String timeoutHandling
    ) {}
}

