package com.tbs.websocket;

import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.exception.BadRequestException;
import com.tbs.exception.ForbiddenException;
import com.tbs.model.Game;
import com.tbs.repository.GameRepository;
import com.tbs.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
public class WebSocketAuthenticationInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthenticationInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final GameRepository gameRepository;

    public WebSocketAuthenticationInterceptor(
            JwtTokenProvider jwtTokenProvider,
            GameRepository gameRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.gameRepository = gameRepository;
    }

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) throws Exception {
        try {
            URI uri = request.getURI();
            log.debug("WebSocket handshake attempt: path={}, query={}", uri.getPath(), uri.getQuery());
            
            String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
            String token = null;
            
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                token = authHeader.substring(BEARER_PREFIX.length()).trim();
                log.debug("WebSocket: Token found in Authorization header");
            } else {
                String query = request.getURI().getQuery();
                if (query != null && query.contains("token=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("token=")) {
                            try {
                                token = java.net.URLDecoder.decode(param.substring(6), "UTF-8");
                            } catch (Exception e) {
                                token = param.substring(6);
                            }
                            log.debug("WebSocket: Token found in query parameter");
                            break;
                        }
                    }
                }
            }
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake rejected: Missing or invalid Authorization header or token query parameter. Path={}, Query={}", uri.getPath(), uri.getQuery());
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
            
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket handshake rejected: Invalid JWT token");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            Long gameId = extractGameIdFromPath(request.getURI());

            log.debug("WebSocket handshake: userId={}, gameId={}, path={}", userId, gameId, uri.getPath());

            if (gameId == null || gameId <= 0) {
                log.warn("WebSocket handshake rejected: Invalid gameId in path. Path={}, gameId={}", uri.getPath(), gameId);
                response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
                return false;
            }

            Game game = gameRepository.findByIdWithPlayers(gameId)
                    .orElseThrow(() -> {
                        log.warn("WebSocket handshake rejected: Game not found, gameId={}", gameId);
                        return new com.tbs.exception.GameNotFoundException("Game not found: " + gameId);
                    });

            try {
                validateGameAccess(game, userId);
            } catch (ForbiddenException e) {
                log.warn("WebSocket handshake rejected: {}", e.getMessage());
                response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                return false;
            } catch (BadRequestException e) {
                log.warn("WebSocket handshake rejected: {}", e.getMessage());
                response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
                return false;
            }

            attributes.put("userId", userId);
            attributes.put("gameId", gameId);
            attributes.put("game", game);

            log.info("WebSocket handshake accepted: userId={}, gameId={}", userId, gameId);
            return true;
        } catch (Exception e) {
            log.error("Error during WebSocket handshake", e);
            response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception
    ) {
        if (exception != null) {
            log.error("WebSocket handshake error", exception);
        }
    }

    private Long extractGameIdFromPath(URI uri) {
        String path = uri.getPath();
        String[] segments = path.split("/");
        
        for (int i = 0; i < segments.length; i++) {
            if ("game".equals(segments[i]) && i + 1 < segments.length) {
                try {
                    return Long.parseLong(segments[i + 1]);
                } catch (NumberFormatException e) {
                    log.warn("Invalid gameId format in path: {}", segments[i + 1]);
                    return null;
                }
            }
        }
        
        return null;
    }

    private void validateGameAccess(Game game, Long userId) {
        if (game.getGameType() != GameType.PVP) {
            throw new BadRequestException("WebSocket is only available for PVP games");
        }

        if (game.getStatus() != GameStatus.IN_PROGRESS && game.getStatus() != GameStatus.WAITING) {
            throw new BadRequestException("Game is not in progress or waiting");
        }

        boolean isPlayer1 = game.getPlayer1() != null && game.getPlayer1().getId().equals(userId);
        boolean isPlayer2 = game.getPlayer2() != null && game.getPlayer2().getId().equals(userId);

        log.debug("Validating game access: gameId={}, userId={}, player1Id={}, player2Id={}, isPlayer1={}, isPlayer2={}", 
                game.getId(), userId, 
                game.getPlayer1() != null ? game.getPlayer1().getId() : null,
                game.getPlayer2() != null ? game.getPlayer2().getId() : null,
                isPlayer1, isPlayer2);

        if (!isPlayer1 && !isPlayer2) {
            log.warn("WebSocket handshake rejected: User {} is not a participant of game {}", userId, game.getId());
            throw new ForbiddenException("You are not a participant of this game");
        }
    }
}

