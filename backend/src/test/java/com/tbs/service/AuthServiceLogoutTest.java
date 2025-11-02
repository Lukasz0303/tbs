package com.tbs.service;

import com.tbs.exception.UnauthorizedException;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import com.tbs.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLogoutSuccessfully() {
        String token = "valid-jwt-token";
        String tokenId = "token-uuid";
        Long userId = 1L;
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(jwtTokenProvider.getTokenId(token)).thenReturn(tokenId);
        when(jwtTokenProvider.getExpirationDateFromToken(token)).thenReturn(expirationTime);
        when(userRepository.updateLastSeenAt(anyLong(), any(Instant.class))).thenReturn(1);

        var result = authService.logout(token);

        assertThat(result.message()).isEqualTo("Wylogowano pomyślnie");
        verify(tokenBlacklistService, times(1)).addToBlacklist(tokenId, expirationTime);
        verify(userRepository, times(1)).updateLastSeenAt(eq(userId), any(Instant.class));
    }

    @Test
    void shouldThrowExceptionWhenTokenIsNull() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token is required");
    }

    @Test
    void shouldThrowExceptionWhenTokenIsEmpty() {
        assertThatThrownBy(() -> authService.logout(""))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token is required");
    }

    @Test
    void shouldHandleBlacklistErrorGracefully() {
        String token = "valid-token";
        String tokenId = "token-uuid";
        Long userId = 1L;
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(jwtTokenProvider.getTokenId(token)).thenReturn(tokenId);
        when(jwtTokenProvider.getExpirationDateFromToken(token)).thenReturn(expirationTime);
        doThrow(new RuntimeException("Redis error")).when(tokenBlacklistService).addToBlacklist(anyString(), any(Date.class));
        when(userRepository.updateLastSeenAt(anyLong(), any(Instant.class))).thenReturn(1);

        var result = authService.logout(token);

        assertThat(result.message()).isEqualTo("Wylogowano pomyślnie");
        verify(userRepository, times(1)).updateLastSeenAt(eq(userId), any(Instant.class));
    }

    @Test
    void shouldHandleUpdateLastSeenErrorGracefully() {
        String token = "valid-token";
        String tokenId = "token-uuid";
        Long userId = 1L;
        Date expirationTime = new Date(System.currentTimeMillis() + 3600000);

        when(authenticationService.getCurrentUserId()).thenReturn(userId);
        when(jwtTokenProvider.getTokenId(token)).thenReturn(tokenId);
        when(jwtTokenProvider.getExpirationDateFromToken(token)).thenReturn(expirationTime);
        doThrow(new RuntimeException("DB error")).when(userRepository).updateLastSeenAt(anyLong(), any(Instant.class));

        var result = authService.logout(token);

        assertThat(result.message()).isEqualTo("Wylogowano pomyślnie");
        verify(tokenBlacklistService, times(1)).addToBlacklist(tokenId, expirationTime);
    }
}

