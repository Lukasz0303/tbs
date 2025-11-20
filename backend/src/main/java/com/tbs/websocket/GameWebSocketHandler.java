package com.tbs.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.tbs.dto.websocket.*;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.InvalidMoveException;
import com.tbs.model.Game;
import com.tbs.model.Move;
import com.tbs.model.User;
import com.tbs.enums.PlayerSymbol;
import com.tbs.repository.GameRepository;
import com.tbs.repository.MoveRepository;
import com.tbs.service.BoardStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private static final int MOVE_TIMEOUT_SECONDS = 20;
    private static final int PING_TIMEOUT_SECONDS = 60;
    private static final int RECONNECT_WINDOW_SECONDS = 20;
    private static final int TIMER_UPDATE_INTERVAL_SECONDS = 1;
    private static final int MAX_MESSAGES_PER_MINUTE = 60;
    private static final int MAX_MOVES_PER_MINUTE = 10;

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final BoardStateService boardStateService;
    private final com.tbs.service.WebSocketMessageStorageService messageStorageService;
    private final com.tbs.service.WebSocketGameService webSocketGameService;
    private final com.tbs.service.RateLimitingService rateLimitingService;
    
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();
    private final Map<Long, Instant> moveDeadlines = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastPingTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public GameWebSocketHandler(
            ObjectMapper objectMapper,
            WebSocketSessionManager sessionManager,
            GameRepository gameRepository,
            MoveRepository moveRepository,
            BoardStateService boardStateService,
            com.tbs.service.WebSocketMessageStorageService messageStorageService,
            com.tbs.service.WebSocketGameService webSocketGameService,
            com.tbs.service.RateLimitingService rateLimitingService,
            @Qualifier("webSocketScheduler") ScheduledExecutorService scheduler
    ) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.boardStateService = boardStateService;
        this.messageStorageService = messageStorageService;
        this.webSocketGameService = webSocketGameService;
        this.rateLimitingService = rateLimitingService;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::cleanupStaleTimers, 60, 60, TimeUnit.SECONDS);
    }

    private void cleanupStaleTimers() {
        try {
            Set<Long> activeGameIds = sessionManager.getAllActiveGameIds();
            gameTimers.entrySet().removeIf(entry -> {
                if (!activeGameIds.contains(entry.getKey())) {
                    ScheduledFuture<?> timer = entry.getValue();
                    if (timer != null && !timer.isDone()) {
                        timer.cancel(false);
                    }
                    return true;
                }
                return false;
            });
            moveDeadlines.entrySet().removeIf(entry -> !activeGameIds.contains(entry.getKey()));
            log.debug("Cleaned up stale timers. Active games: {}", activeGameIds.size());
        } catch (Exception e) {
            log.error("Error during cleanup of stale timers", e);
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long gameId = (Long) session.getAttributes().get("gameId");
        Long userId = (Long) session.getAttributes().get("userId");
        Game game = (Game) session.getAttributes().get("game");

        if (gameId == null || userId == null || game == null) {
            log.error("WebSocket connection established with missing attributes");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (game.getStatus() == com.tbs.enums.GameStatus.FINISHED || 
            game.getStatus() == com.tbs.enums.GameStatus.ABANDONED) {
            log.warn("WebSocket connection attempted for finished game: gameId={}, status={}", 
                    gameId, game.getStatus());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        activeSessions.put(session.getId(), session);
        sessionManager.addSession(gameId, userId, session.getId());
        lastPingTime.put(session.getId(), Instant.now());
        log.info("WebSocket connection established: gameId={}, userId={}, sessionId={}", 
                gameId, userId, session.getId());
        log.debug("Active sessions after add: {}", activeSessions.keySet());
        log.debug("SessionManager sessions for gameId={}: {}", gameId, sessionManager.getGameSessions(gameId));

        sendInitialGameState(session, game);
        
        if (game.getStatus() == com.tbs.enums.GameStatus.IN_PROGRESS && game.getCurrentPlayerSymbol() != null) {
            startMoveTimer(gameId, game);
        }
        
        startPingTimeoutCheck(session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long gameId = (Long) session.getAttributes().get("gameId");
        Long userId = (Long) session.getAttributes().get("userId");

        if (gameId == null || userId == null) {
            log.error("Received message from session with missing attributes");
            return;
        }

        final int MAX_PAYLOAD_SIZE = 1024;
        if (message.getPayloadLength() > MAX_PAYLOAD_SIZE) {
            log.warn("WebSocket message too large: {} bytes (max: {})", message.getPayloadLength(), MAX_PAYLOAD_SIZE);
            sendError(session, "Message too large");
            return;
        }

        String rateLimitKey = "websocket:" + userId + ":" + gameId;
        if (!rateLimitingService.isAllowed(rateLimitKey + ":messages", MAX_MESSAGES_PER_MINUTE, java.time.Duration.ofMinutes(1))) {
            log.warn("Rate limit exceeded for WebSocket messages: userId={}, gameId={}", userId, gameId);
            sendError(session, "Rate limit exceeded. Please slow down.");
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            if (root == null || !root.hasNonNull("type")) {
                log.error("WebSocket message missing type field: gameId={}, userId={}, payload={}", gameId, userId, message.getPayload());
                sendError(session, "Invalid message format");
                return;
            }

            String typeText = root.get("type").asText();
            WebSocketMessageType type;
            try {
                type = WebSocketMessageType.valueOf(typeText);
            } catch (IllegalArgumentException e) {
                log.error("Unknown WebSocket message type: gameId={}, userId={}, type={}, payload={}", gameId, userId, typeText, message.getPayload());
                sendError(session, "Invalid message format");
                return;
            }

            switch (type) {
                case MOVE -> {
                    String moveRateLimitKey = rateLimitKey + ":moves";
                    if (!rateLimitingService.isAllowed(moveRateLimitKey, MAX_MOVES_PER_MINUTE, java.time.Duration.ofMinutes(1))) {
                        log.warn("Rate limit exceeded for WebSocket moves: userId={}, gameId={}", userId, gameId);
                        sendError(session, "Move rate limit exceeded. Please slow down.");
                        return;
                    }
                    MoveMessage moveMessage = objectMapper.treeToValue(root, MoveMessage.class);
                    handleMove(session, gameId, userId, moveMessage);
                }
                case SURRENDER -> handleSurrender(session, gameId, userId);
                case PING -> {
                    PingMessage pingMessage = objectMapper.treeToValue(root, PingMessage.class);
                    handlePing(session, pingMessage);
                }
                default -> {
                    log.warn("Unsupported client WebSocket message type: gameId={}, userId={}, type={}", gameId, userId, type);
                    sendError(session, "Invalid message format");
                }
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message: gameId={}, userId={}, payload={}", gameId, userId, message.getPayload(), e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Long gameId = (Long) session.getAttributes().get("gameId");
        Long userId = (Long) session.getAttributes().get("userId");

        if (gameId != null && userId != null) {
            log.info("WebSocket connection closing: gameId={}, userId={}, sessionId={}, status={}", 
                    gameId, userId, session.getId(), status);
            lastPingTime.remove(session.getId());
            activeSessions.remove(session.getId());
            sessionManager.removeSession(gameId, userId);
            log.info("WebSocket connection closed: gameId={}, userId={}, sessionId={}", 
                    gameId, userId, session.getId());
            log.debug("Active sessions after remove: {}", activeSessions.keySet());
            log.debug("SessionManager sessions for gameId={}: {}", gameId, sessionManager.getGameSessions(gameId));
            
            handleDisconnection(gameId, userId);
        }
    }

    private void handleMessage(WebSocketSession session, Long gameId, Long userId, BaseWebSocketMessage message) {
        switch (message.type()) {
            case MOVE -> handleMove(session, gameId, userId, (MoveMessage) message);
            case SURRENDER -> handleSurrender(session, gameId, userId);
            case PING -> handlePing(session, (PingMessage) message);
            default -> log.warn("Unknown message type: {}", message.type());
        }
    }

    private void sendInitialGameState(WebSocketSession session, Game game) throws IOException {
        List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(game.getId());
        com.tbs.dto.common.BoardState boardState = boardStateService.generateBoardState(game, moves);

        GameUpdateMessage gameUpdate = new GameUpdateMessage(
                new GameUpdateMessage.GameUpdatePayload(
                        game.getId(),
                        game.getStatus(),
                        game.getWinner() != null 
                                ? new com.tbs.dto.user.WinnerInfo(game.getWinner().getId(), game.getWinner().getUsername())
                                : null,
                        boardState
                )
        );

        sendMessage(session, gameUpdate);
    }

    private void handleMove(WebSocketSession session, Long gameId, Long userId, MoveMessage message) {
        try {
            com.tbs.service.WebSocketGameService.MoveResult result = webSocketGameService.processMove(
                    gameId,
                    userId,
                    message.payload().row(),
                    message.payload().col(),
                    message.payload().playerSymbol()
            );

            Move savedMove = result.move();
            Game updatedGame = result.game();
            com.tbs.dto.common.BoardState boardState = result.boardState();

            MoveAcceptedMessage acceptedMessage = new MoveAcceptedMessage(
                    new MoveAcceptedMessage.MoveAcceptedPayload(
                            savedMove.getId(),
                            message.payload().row(),
                            message.payload().col(),
                            message.payload().playerSymbol(),
                            boardState,
                            updatedGame.getCurrentPlayerSymbol(),
                            Instant.now().plusSeconds(MOVE_TIMEOUT_SECONDS)
                    )
            );

            sendMessage(session, acceptedMessage);

            sendOpponentMove(gameId, userId, message.payload().row(), message.payload().col(), 
                    message.payload().playerSymbol(), boardState, updatedGame);

            stopMoveTimer(gameId);
            
            if (updatedGame.getStatus() == com.tbs.enums.GameStatus.FINISHED || 
                updatedGame.getStatus() == com.tbs.enums.GameStatus.DRAW) {
                handleGameEnded(gameId, updatedGame, boardState, result.totalMoves());
            } else {
                startMoveTimer(gameId, updatedGame);
            }

        } catch (InvalidMoveException | ForbiddenException e) {
            log.warn("Move rejected: gameId={}, userId={}, reason={}", gameId, userId, e.getMessage());
            MoveRejectedMessage rejectedMessage = new MoveRejectedMessage(
                    new MoveRejectedMessage.MoveRejectedPayload(
                            e.getMessage(),
                            getErrorCode(e)
                    )
            );
            sendMessage(session, rejectedMessage);
        } catch (Exception e) {
            log.error("Error processing move: gameId={}, userId={}", gameId, userId, e);
            MoveRejectedMessage rejectedMessage = new MoveRejectedMessage(
                    new MoveRejectedMessage.MoveRejectedPayload(
                            "Internal server error",
                            "MOVE_INVALID_SERVER_ERROR"
                    )
            );
            sendMessage(session, rejectedMessage);
        }
    }

    private void handleSurrender(WebSocketSession session, Long gameId, Long userId) {
        try {
            com.tbs.service.WebSocketGameService.SurrenderResult result = webSocketGameService.processSurrender(gameId, userId);

            Game game = result.game();
            com.tbs.dto.user.WinnerInfo winnerInfo = new com.tbs.dto.user.WinnerInfo(
                    result.winner().getId(),
                    result.winner().getUsername()
            );

            GameEndedMessage gameEnded = new GameEndedMessage(
                    new GameEndedMessage.GameEndedPayload(
                            gameId,
                            game.getStatus(),
                            winnerInfo,
                            result.boardState(),
                            result.totalMoves()
                    )
            );

            sendMessageToBothPlayers(gameId, gameEnded);
            closeBothSessions(gameId);

        } catch (Exception e) {
            log.error("Error processing surrender: gameId={}, userId={}", gameId, userId, e);
        }
    }

    private void handlePing(WebSocketSession session, PingMessage message) {
        lastPingTime.put(session.getId(), Instant.now());
        PongMessage pong = new PongMessage(new PongMessage.PongPayload(message.payload().timestamp()));
        sendMessage(session, pong);
    }

    private PlayerSymbol determinePlayer1SymbolFromMoves(Game game, List<Move> existingMoves) {
        if (!existingMoves.isEmpty()) {
            for (Move move : existingMoves) {
                if (move.getPlayer() != null && move.getPlayer().getId().equals(game.getPlayer1().getId())) {
                    return move.getPlayerSymbol();
                }
            }
        }
        return PlayerSymbol.X;
    }

    private void sendOpponentMove(Long gameId, Long userId, int row, int col, 
                                  PlayerSymbol playerSymbol,
                                  com.tbs.dto.common.BoardState boardState, Game game) {
        Map<Long, String> sessions = new HashMap<>(sessionManager.getGameSessions(gameId));
        
        for (Map.Entry<Long, String> entry : sessions.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                String sessionId = entry.getValue();
                WebSocketSession opponentSession = activeSessions.get(sessionId);
                if (opponentSession != null && opponentSession.isOpen()) {
                    OpponentMoveMessage opponentMove = new OpponentMoveMessage(
                            new OpponentMoveMessage.OpponentMovePayload(
                                    row,
                                    col,
                                    playerSymbol,
                                    boardState,
                                    game.getCurrentPlayerSymbol(),
                                    Instant.now().plusSeconds(MOVE_TIMEOUT_SECONDS)
                            )
                    );
                    sendMessage(opponentSession, opponentMove);
                }
            }
        }
    }

    private void sendMessageToBothPlayers(Long gameId, BaseWebSocketMessage message) {
        Map<Long, String> sessions = new HashMap<>(sessionManager.getGameSessions(gameId));
        sessions.forEach((userId, sessionId) -> {
            WebSocketSession session = activeSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }

    private void handleGameEnded(Long gameId, Game game, com.tbs.dto.common.BoardState boardState, int totalMoves) {
        com.tbs.dto.user.WinnerInfo winnerInfo = null;
        if (game.getWinner() != null) {
            winnerInfo = new com.tbs.dto.user.WinnerInfo(game.getWinner().getId(), game.getWinner().getUsername());
        }

        GameEndedMessage gameEnded = new GameEndedMessage(
                new GameEndedMessage.GameEndedPayload(
                        gameId,
                        game.getStatus(),
                        winnerInfo,
                        boardState,
                        totalMoves
                )
        );

        sendMessageToBothPlayers(gameId, gameEnded);
        closeBothSessions(gameId);
    }

    private void sendMessage(WebSocketSession session, BaseWebSocketMessage message) {
        try {
            Long gameId = (Long) session.getAttributes().get("gameId");
            Long userId = (Long) session.getAttributes().get("userId");
            
            String json = objectMapper.writeValueAsString(message);
            log.debug("sendMessage called: sessionId={}, messageType={}, sessionOpen={}, gameId={}, userId={}", 
                    session.getId(), message.type(), session.isOpen(), gameId, userId);
            
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
                log.debug("WebSocket message sent: type={}, sessionId={}", 
                        message.type(), session.getId());
                
                if (gameId != null && userId != null) {
                    messageStorageService.storeMessage(gameId, userId, message);
                    log.debug("Stored WebSocket message for gameId={}, userId={}, type={}", 
                            gameId, userId, message.type());
                }
            } else {
                log.warn("Cannot send WebSocket message: session is CLOSED, sessionId={}", 
                        session.getId());
            }
        } catch (IOException e) {
            log.error("EXCEPTION sending WebSocket message: sessionId={}, messageType={}, error={}", 
                    session.getId(), message.type(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("UNEXPECTED EXCEPTION sending WebSocket message: sessionId={}, messageType={}, error={}", 
                    session.getId(), message.type(), e.getMessage(), e);
        }
    }

    public void notifyMoveFromRestApi(Long gameId, Long userId, Long moveId, int row, int col,
                                      PlayerSymbol playerSymbol,
                                      com.tbs.dto.common.BoardState boardState,
                                      PlayerSymbol currentPlayerSymbol,
                                      com.tbs.enums.GameStatus gameStatus) {
        try {
            log.debug("notifyMoveFromRestApi: gameId={}, userId={}, moveId={}, row={}, col={}, symbol={}", 
                    gameId, userId, moveId, row, col, playerSymbol);
            Map<Long, String> sessions = new HashMap<>(sessionManager.getGameSessions(gameId));
            
            log.debug("WebSocket sessions for gameId={}: {}", gameId, sessions);
            
            if (sessions.isEmpty()) {
                log.debug("No WebSocket sessions found for gameId={}, skipping notification", gameId);
                return;
            }
            
            log.debug("Processing {} WebSocket session(s) for game {}", sessions.size(), gameId);

            MoveAcceptedMessage acceptedMessage = new MoveAcceptedMessage(
                    new MoveAcceptedMessage.MoveAcceptedPayload(
                            moveId,
                            row,
                            col,
                            playerSymbol,
                            boardState,
                            currentPlayerSymbol,
                            Instant.now().plusSeconds(MOVE_TIMEOUT_SECONDS)
                    )
            );

            String moverSessionId = sessions.get(userId);
            log.debug("Mover userId={}, sessionId={}", userId, moverSessionId);
            
            if (moverSessionId != null) {
                WebSocketSession moverSession = activeSessions.get(moverSessionId);
                if (moverSession != null && moverSession.isOpen()) {
                    sendMessage(moverSession, acceptedMessage);
                    log.debug("MOVE_ACCEPTED sent to mover userId={}", userId);
                } else {
                    log.warn("Mover session not found or closed: userId={}, sessionId={}", 
                            userId, moverSessionId);
                }
            }

            for (Map.Entry<Long, String> entry : sessions.entrySet()) {
                if (!entry.getKey().equals(userId)) {
                    Long opponentUserId = entry.getKey();
                    String opponentSessionId = entry.getValue();
                    
                    WebSocketSession opponentSession = activeSessions.get(opponentSessionId);
                    
                    if (opponentSession != null && opponentSession.isOpen()) {
                        OpponentMoveMessage opponentMove = new OpponentMoveMessage(
                                new OpponentMoveMessage.OpponentMovePayload(
                                        row,
                                        col,
                                        playerSymbol,
                                        boardState,
                                        currentPlayerSymbol,
                                        Instant.now().plusSeconds(MOVE_TIMEOUT_SECONDS)
                                )
                        );
                        sendMessage(opponentSession, opponentMove);
                        log.debug("OPPONENT_MOVE sent to opponent userId={}", opponentUserId);
                    } else {
                        log.warn("Opponent session not found or closed: userId={}, sessionId={}", 
                                opponentUserId, opponentSessionId);
                    }
                }
            }

            Game currentGame = gameRepository.findByIdWithPlayers(gameId)
                    .orElseThrow(() -> new com.tbs.exception.GameNotFoundException("Game not found: " + gameId));
            
            stopMoveTimer(gameId);
            
            if (gameStatus == com.tbs.enums.GameStatus.FINISHED || 
                gameStatus == com.tbs.enums.GameStatus.DRAW) {
                List<Move> allMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
                handleGameEnded(gameId, currentGame, boardState, allMoves.size());
            } else if (gameStatus == com.tbs.enums.GameStatus.IN_PROGRESS) {
                startMoveTimer(gameId, currentGame);
            }
        } catch (Exception e) {
            log.error("Error notifying WebSocket about move from REST API: gameId={}, userId={}", 
                    gameId, userId, e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            MoveRejectedMessage errorMessage = new MoveRejectedMessage(
                    new MoveRejectedMessage.MoveRejectedPayload(error, "ERROR")
            );
            sendMessage(session, errorMessage);
        } catch (Exception e) {
            log.error("Error sending error message", e);
        }
    }

    private String getErrorCode(Exception e) {
        if (e instanceof InvalidMoveException) {
            String message = e.getMessage();
            if (message.contains("already occupied")) {
                return "MOVE_INVALID_OCCUPIED";
            }
            if (message.contains("out of bounds")) {
                return "MOVE_INVALID_OUT_OF_BOUNDS";
            }
            if (message.contains("turn")) {
                return "MOVE_INVALID_NOT_YOUR_TURN";
            }
        }
        if (e instanceof ForbiddenException) {
            return "MOVE_INVALID_NOT_YOUR_TURN";
        }
        return "MOVE_INVALID_UNKNOWN";
    }

    private void startMoveTimer(Long gameId, Game game) {
        stopMoveTimer(gameId);
        
        Instant deadline = Instant.now().plusSeconds(MOVE_TIMEOUT_SECONDS);
        moveDeadlines.put(gameId, deadline);
        
        ScheduledFuture<?> timerTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                updateTimer(gameId, game);
            } catch (Exception e) {
                log.error("Error in move timer for gameId={}", gameId, e);
            }
        }, 0, TIMER_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        gameTimers.put(gameId, timerTask);
        
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            try {
                handleMoveTimeout(gameId, game);
            } catch (Exception e) {
                log.error("Error handling move timeout for gameId={}", gameId, e);
            }
        }, MOVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        gameTimers.put(gameId, timeoutTask);
    }

    private void stopMoveTimer(Long gameId) {
        ScheduledFuture<?> timer = gameTimers.remove(gameId);
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
        }
        moveDeadlines.remove(gameId);
    }

    private void updateTimer(Long gameId, Game game) {
        Instant deadline = moveDeadlines.get(gameId);
        if (deadline == null) {
            return;
        }
        
        long remainingSeconds = java.time.Duration.between(Instant.now(), deadline).getSeconds();
        
        if (remainingSeconds <= 0) {
            return;
        }
        
        try {
            Game currentGame = gameRepository.findByIdWithPlayers(gameId)
                    .orElseThrow(() -> new com.tbs.exception.GameNotFoundException("Game not found: " + gameId));
            
            if (currentGame.getStatus() != com.tbs.enums.GameStatus.IN_PROGRESS) {
                stopMoveTimer(gameId);
                return;
            }
            
            if (currentGame.getCurrentPlayerSymbol() == null) {
                return;
            }
            
            TimerUpdateMessage timerUpdate = new TimerUpdateMessage(
                    new TimerUpdateMessage.TimerUpdatePayload(
                            (int) remainingSeconds,
                            currentGame.getCurrentPlayerSymbol()
                    )
            );
            
            sendMessageToBothPlayers(gameId, timerUpdate);
        } catch (com.tbs.exception.GameNotFoundException e) {
            log.warn("Game not found during timer update: gameId={}", gameId);
            stopMoveTimer(gameId);
        } catch (Exception e) {
            log.error("Error during timer update: gameId={}", gameId, e);
            stopMoveTimer(gameId);
        }
    }

    private void handleMoveTimeout(Long gameId, Game game) {
        try {
            Game currentGame = gameRepository.findByIdWithPlayers(gameId)
                    .orElseThrow(() -> new com.tbs.exception.GameNotFoundException("Game not found: " + gameId));
            
            if (currentGame.getStatus() != com.tbs.enums.GameStatus.IN_PROGRESS) {
                stopMoveTimer(gameId);
                return;
            }
        
            if (currentGame.getCurrentPlayerSymbol() == null) {
                stopMoveTimer(gameId);
                return;
            }
            
            PlayerSymbol currentPlayerSymbol = currentGame.getCurrentPlayerSymbol();
            
            List<Move> existingMoves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
            PlayerSymbol player1Symbol = determinePlayer1SymbolFromMoves(currentGame, existingMoves);
            
            User winner = (currentPlayerSymbol == player1Symbol) 
                    ? currentGame.getPlayer2() 
                    : currentGame.getPlayer1();
            
            finishGameWithWinner(gameId, winner, "move timeout");
        } catch (com.tbs.exception.GameNotFoundException e) {
            log.warn("Game not found during move timeout: gameId={}", gameId);
            stopMoveTimer(gameId);
        } catch (Exception e) {
            log.error("Error handling move timeout: gameId={}", gameId, e);
            stopMoveTimer(gameId);
        }
    }

    private void startPingTimeoutCheck(WebSocketSession session) {
        scheduler.schedule(() -> {
            try {
                Instant lastPing = lastPingTime.get(session.getId());
                if (lastPing != null && 
                    java.time.Duration.between(lastPing, Instant.now()).getSeconds() > PING_TIMEOUT_SECONDS) {
                    log.warn("Ping timeout for session: {}", session.getId());
                    if (session.isOpen()) {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking ping timeout", e);
            }
        }, PING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void handleDisconnection(Long gameId, Long userId) {
        scheduler.schedule(() -> {
            try {
                Map<Long, String> sessions = new HashMap<>(sessionManager.getGameSessions(gameId));
                if (sessions.containsKey(userId)) {
                    log.info("Reconnect window expired for gameId={}, userId={}. Forfeiting game.", gameId, userId);
                    
                    try {
                        Game game = gameRepository.findByIdWithPlayers(gameId)
                                .orElseThrow(() -> new com.tbs.exception.GameNotFoundException("Game not found: " + gameId));
                        
                        if (game.getStatus() != com.tbs.enums.GameStatus.IN_PROGRESS) {
                            return;
                        }
                        
                        User winner = game.getPlayer1().getId().equals(userId) 
                                ? game.getPlayer2() 
                                : game.getPlayer1();
                        
                        if (winner == null) {
                            log.warn("Cannot determine winner for disconnection in gameId={}, userId={}", gameId, userId);
                            return;
                        }
                        
                        finishGameWithWinner(gameId, winner, "disconnection");
                    } catch (com.tbs.exception.GameNotFoundException e) {
                        log.warn("Game not found during disconnection: gameId={}", gameId);
                    }
                }
            } catch (Exception e) {
                log.error("Error handling disconnection for gameId={}, userId={}", gameId, userId, e);
            }
        }, RECONNECT_WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    private void finishGameWithWinner(Long gameId, User winner, String reason) {
        try {
            Game game = gameRepository.findByIdWithPlayers(gameId)
                    .orElseThrow(() -> new com.tbs.exception.GameNotFoundException("Game not found: " + gameId));
            
            if (game.getStatus() != com.tbs.enums.GameStatus.IN_PROGRESS) {
                return;
            }
            
            if (winner == null) {
                log.error("Cannot determine winner for {} in gameId={}", reason, gameId);
                return;
            }
            
            game.setStatus(com.tbs.enums.GameStatus.FINISHED);
            game.setWinner(winner);
            game.setFinishedAt(Instant.now());
            gameRepository.save(game);
            
            stopMoveTimer(gameId);
            
            List<Move> moves = moveRepository.findByGameIdOrderByMoveOrderAsc(gameId);
            com.tbs.dto.common.BoardState boardState = boardStateService.generateBoardState(game, moves);
            
            log.info("Game {} finished: {}. Winner: user {}", gameId, reason, winner.getId());
            handleGameEnded(gameId, game, boardState, moves.size());
        } catch (com.tbs.exception.GameNotFoundException e) {
            log.warn("Game not found during finishGameWithWinner: gameId={}, reason={}", gameId, reason);
        } catch (Exception e) {
            log.error("Error finishing game: gameId={}, reason={}", gameId, reason, e);
        }
    }

    private void closeBothSessions(Long gameId) {
        stopMoveTimer(gameId);
        Map<Long, String> sessions = new HashMap<>(sessionManager.getGameSessions(gameId));
        sessions.forEach((userId, sessionId) -> {
            WebSocketSession session = activeSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (IOException e) {
                    log.error("Error closing WebSocket session", e);
                }
            }
            activeSessions.remove(sessionId);
            lastPingTime.remove(sessionId);
        });
        sessionManager.removeAllGameSessions(gameId);
    }
    
}

