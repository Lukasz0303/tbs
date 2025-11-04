package com.tbs.service;

import com.tbs.dto.user.LastSeenResponse;
import com.tbs.dto.user.UpdateUserRequest;
import com.tbs.dto.user.UpdateUserResponse;
import com.tbs.dto.user.UserProfileResponse;
import com.tbs.exception.ConflictException;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.UserNotFoundException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private UserService userService;

    private User registeredUser;
    private User guestUser;

    @BeforeEach
    void setUp() {
        registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setUsername("testuser");
        registeredUser.setIsGuest(false);
        registeredUser.setTotalPoints(100L);
        registeredUser.setGamesPlayed(10);
        registeredUser.setGamesWon(5);
        registeredUser.setCreatedAt(Instant.now());
        registeredUser.setLastSeenAt(Instant.now());

        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setUsername(null);
        guestUser.setIsGuest(true);
        guestUser.setTotalPoints(50L);
        guestUser.setGamesPlayed(5);
        guestUser.setGamesWon(2);
        guestUser.setCreatedAt(Instant.now());
        guestUser.setLastSeenAt(Instant.now());
    }

    @Test
    void getUserProfile_shouldReturnProfileForRegisteredUser() {
        Long userId = 1L;
        Long currentUserId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        UserProfileResponse response = userService.getUserProfile(userId, currentUserId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.isGuest()).isFalse();
        assertThat(response.totalPoints()).isEqualTo(100L);
        assertThat(response.gamesPlayed()).isEqualTo(10);
        assertThat(response.gamesWon()).isEqualTo(5);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_shouldReturnProfileForGuestUserWhenOwner() {
        Long userId = 2L;
        Long currentUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(guestUser));

        UserProfileResponse response = userService.getUserProfile(userId, currentUserId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.username()).isNull();
        assertThat(response.isGuest()).isTrue();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_shouldThrowForbiddenWhenGuestUserNotOwner() {
        Long userId = 2L;
        Long currentUserId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> userService.getUserProfile(userId, currentUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied to guest profile");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_shouldThrowForbiddenWhenGuestUserUnauthenticated() {
        Long userId = 2L;
        Long currentUserId = null;

        when(userRepository.findById(userId)).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> userService.getUserProfile(userId, currentUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied to guest profile");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_shouldThrowUserNotFoundException() {
        Long userId = 999L;
        Long currentUserId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(userId, currentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updateLastSeen_shouldUpdateTimestampSuccessfully() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(userRepository.save(any(User.class))).thenReturn(registeredUser);

        LastSeenResponse response = userService.updateLastSeen(userId);

        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("Last seen updated successfully");
        assertThat(response.lastSeenAt()).isNotNull();
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateLastSeen_shouldThrowUserNotFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateLastSeen(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_shouldUpdateUsernameSuccessfully() {
        Long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(registeredUser);

        UpdateUserResponse response = userService.updateUserProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByUsername("newusername");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserProfile_shouldNotUpdateWhenUsernameUnchanged() {
        Long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest("testuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(userRepository.save(any(User.class))).thenReturn(registeredUser);

        UpdateUserResponse response = userService.updateUserProfile(userId, request);

        assertThat(response).isNotNull();
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserProfile_shouldThrowConflictExceptionWhenUsernameExists() {
        Long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest("existinguser");
        User existingUser = new User();
        existingUser.setId(999L);
        existingUser.setUsername("existinguser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already exists");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_shouldThrowUserNotFoundException() {
        Long userId = 999L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}

