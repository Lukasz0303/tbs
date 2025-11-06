package com.tbs.config;

import com.tbs.websocket.WebSocketAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final com.tbs.websocket.GameWebSocketHandler gameWebSocketHandler;
    private final WebSocketAuthenticationInterceptor authenticationInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            com.tbs.websocket.GameWebSocketHandler gameWebSocketHandler,
            WebSocketAuthenticationInterceptor authenticationInterceptor,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.authenticationInterceptor = authenticationInterceptor;
        this.allowedOrigins = Arrays.asList(allowedOrigins.split(","));
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game/{gameId}")
                .addInterceptors(authenticationInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}

