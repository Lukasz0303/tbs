package com.tbs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("World at War: Turn-Based Strategy API")
                        .version("1.0.0")
                        .description("API for World at War - competitive turn-based strategy game\n\n" +
                                "## WebSocket Endpoints\n\n" +
                                "**WebSocket endpointy nie są standardowo dokumentowane w OpenAPI**, ale są dostępne:\n\n" +
                                "### WS /ws/game/{gameId}\n\n" +
                                "Połączenie WebSocket dla komunikacji w czasie rzeczywistym podczas rozgrywki PvP.\n\n" +
                                "**Połączenie:**\n" +
                                "- URL: `ws://localhost:8080/ws/game/{gameId}`\n" +
                                "- Protokół: WebSocket\n" +
                                "- Autoryzacja: JWT token w nagłówku `Authorization: Bearer <token>`\n\n" +
                                "**Wymagania:**\n" +
                                "- Gracz musi być uczestnikiem gry (player1_id lub player2_id)\n" +
                                "- Typ gry musi być PVP\n" +
                                "- Status gry musi być IN_PROGRESS\n\n" +
                                "**Typy wiadomości:**\n" +
                                "- **MOVE** - Wysłanie ruchu gracza\n" +
                                "- **SURRENDER** - Poddanie gry\n" +
                                "- **PING** - Keep-alive\n\n" +
                                "**Odpowiedzi:**\n" +
                                "- **MOVE_ACCEPTED** - Ruch zaakceptowany\n" +
                                "- **MOVE_REJECTED** - Ruch odrzucony\n" +
                                "- **OPPONENT_MOVE** - Ruch przeciwnika\n" +
                                "- **GAME_UPDATE** - Aktualizacja stanu gry\n" +
                                "- **TIMER_UPDATE** - Aktualizacja timera\n" +
                                "- **GAME_ENDED** - Gra zakończona\n" +
                                "- **PONG** - Odpowiedź keep-alive\n\n" +
                                "**Szczegółowa dokumentacja:** Zobacz plik `.ai/implementation-plans/websocket/ws-game-gameId.md`"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT authorization token")
                        ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

