package com.tbs.service;

import com.tbs.dto.guest.GuestResponse;
import com.tbs.exception.BadRequestException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IpAddressService ipAddressService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private GuestService guestService;

    private User guestUser;
    private String testIpAddress = "192.168.1.1";
    private String testToken = "test-jwt-token";

    @BeforeEach
    void setUp() {
        guestUser = new User();
        guestUser.setId(1L);
        guestUser.setIsGuest(true);
        guestUser.setIpAddress(testIpAddress);
        guestUser.setTotalPoints(0L);
        guestUser.setGamesPlayed(0);
        guestUser.setGamesWon(0);
        guestUser.setCreatedAt(Instant.now());
    }

    @Test
    void findOrCreateGuest_shouldReturnExistingGuest_whenGuestExists() {
        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.of(guestUser));
        when(jwtTokenProvider.generateToken(1L)).thenReturn(testToken);

        GuestResponse response = guestService.findOrCreateGuest(testIpAddress);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.isGuest()).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findOrCreateGuest_shouldCreateNewGuest_whenGuestDoesNotExist() {
        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(guestUser);
        when(jwtTokenProvider.generateToken(1L)).thenReturn(testToken);

        GuestResponse response = guestService.findOrCreateGuest(testIpAddress);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.isGuest()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreateGuest_shouldHandleRaceCondition_whenConcurrentRequests() {
        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(guestUser));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(jwtTokenProvider.generateToken(1L)).thenReturn(testToken);

        GuestResponse response = guestService.findOrCreateGuest(testIpAddress);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.isGuest()).isTrue();
        verify(userRepository, times(2)).findByIpAddressAndIsGuest(testIpAddress, true);
    }

    @Test
    void findOrCreateGuest_shouldThrowException_whenInvalidIpAddress() {
        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(false);

        assertThatThrownBy(() -> guestService.findOrCreateGuest(testIpAddress))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid IP address format");
        verify(userRepository, never()).findByIpAddressAndIsGuest(anyString(), anyBoolean());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findOrCreateGuest_shouldThrowException_whenRaceConditionAndGuestNotFound() {
        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestService.findOrCreateGuest(testIpAddress))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to create or retrieve guest profile");
    }

    @Test
    void findOrCreateGuest_shouldHandleNullValues_whenMappingToResponse() {
        User userWithNulls = new User();
        userWithNulls.setId(2L);
        userWithNulls.setIsGuest(true);
        userWithNulls.setIpAddress(testIpAddress);
        userWithNulls.setTotalPoints(null);
        userWithNulls.setGamesPlayed(null);
        userWithNulls.setGamesWon(null);
        userWithNulls.setCreatedAt(null);

        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.of(userWithNulls));
        when(jwtTokenProvider.generateToken(2L)).thenReturn(testToken);

        GuestResponse response = guestService.findOrCreateGuest(testIpAddress);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.totalPoints()).isEqualTo(0L);
        assertThat(response.gamesPlayed()).isEqualTo(0);
        assertThat(response.gamesWon()).isEqualTo(0);
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void findOrCreateGuest_shouldThrowException_whenUserIdIsNull() {
        User userWithoutId = new User();
        userWithoutId.setId(null);
        userWithoutId.setIsGuest(true);

        when(ipAddressService.isValidIpAddress(testIpAddress)).thenReturn(true);
        when(userRepository.findByIpAddressAndIsGuest(testIpAddress, true))
                .thenReturn(Optional.of(userWithoutId));

        assertThatThrownBy(() -> guestService.findOrCreateGuest(testIpAddress))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User ID cannot be null");
    }
}

