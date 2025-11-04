package com.tbs.controller;

import com.tbs.dto.user.LastSeenResponse;
import com.tbs.dto.user.UpdateUserRequest;
import com.tbs.dto.user.UpdateUserResponse;
import com.tbs.dto.user.UserProfileResponse;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.UserNotFoundException;
import com.tbs.service.AuthenticationService;
import com.tbs.service.IpAddressService;
import com.tbs.service.RateLimitingService;
import com.tbs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private IpAddressService ipAddressService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        when(rateLimitingService.isAllowed(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString(), anyInt()))
                .thenReturn(100L);
        when(rateLimitingService.getTimeToReset(anyString()))
                .thenReturn(Duration.ofMinutes(1));
        when(ipAddressService.getClientIpAddress(any(HttpServletRequest.class)))
                .thenReturn("127.0.0.1");
    }

    @Test
    void getUserProfile_shouldReturnProfileSuccessfully() {
        Long userId = 1L;
        UserProfileResponse profileResponse = new UserProfileResponse(
                1L, "testuser", false, 100L, 10, 5, Instant.now()
        );

        when(authenticationService.getCurrentUserIdOrNull()).thenReturn(1L);
        when(userService.getUserProfile(userId, 1L)).thenReturn(profileResponse);

        ResponseEntity<UserProfileResponse> response = userController.getUserProfile(userId, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(1L);
        assertThat(response.getBody().username()).isEqualTo("testuser");
        verify(authenticationService, times(1)).getCurrentUserIdOrNull();
        verify(userService, times(1)).getUserProfile(userId, 1L);
        verify(rateLimitingService, times(1)).isAllowed(anyString(), eq(100), any(Duration.class));
    }

    @Test
    void getUserProfile_shouldReturnProfileForUnauthenticatedUser() {
        Long userId = 1L;
        UserProfileResponse profileResponse = new UserProfileResponse(
                1L, "testuser", false, 100L, 10, 5, Instant.now()
        );

        when(authenticationService.getCurrentUserIdOrNull()).thenReturn(null);
        when(userService.getUserProfile(userId, null)).thenReturn(profileResponse);

        ResponseEntity<UserProfileResponse> response = userController.getUserProfile(userId, httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(authenticationService, times(1)).getCurrentUserIdOrNull();
        verify(userService, times(1)).getUserProfile(userId, null);
        verify(rateLimitingService, times(1)).isAllowed(anyString(), eq(100), any(Duration.class));
    }

    @Test
    void getUserProfile_shouldThrowUserNotFoundException() {
        Long userId = 999L;

        when(authenticationService.getCurrentUserIdOrNull()).thenReturn(null);
        when(userService.getUserProfile(userId, null))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThatThrownBy(() -> userController.getUserProfile(userId, httpServletRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(authenticationService, times(1)).getCurrentUserIdOrNull();
        verify(userService, times(1)).getUserProfile(userId, null);
        verify(rateLimitingService, times(1)).isAllowed(anyString(), eq(100), any(Duration.class));
    }

    @Test
    void updateLastSeen_shouldUpdateTimestampSuccessfully() {
        Long userId = 1L;
        Instant now = Instant.now();
        LastSeenResponse lastSeenResponse = new LastSeenResponse(
                "Last seen updated successfully", now
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(userService.updateLastSeen(userId)).thenReturn(lastSeenResponse);

        ResponseEntity<LastSeenResponse> response = userController.updateLastSeen(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Last seen updated successfully");
        assertThat(response.getBody().lastSeenAt()).isNotNull();
        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, times(1)).updateLastSeen(userId);
    }

    @Test
    void updateLastSeen_shouldThrowForbiddenExceptionWhenNotOwner() {
        Long userId = 2L;

        when(authenticationService.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> userController.updateLastSeen(userId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can only update your own last seen timestamp");

        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, never()).updateLastSeen(anyLong());
    }

    @Test
    void updateLastSeen_shouldThrowUserNotFoundException() {
        Long userId = 999L;

        when(authenticationService.getCurrentUserId()).thenReturn(999L);
        when(userService.updateLastSeen(userId))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThatThrownBy(() -> userController.updateLastSeen(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, times(1)).updateLastSeen(userId);
    }

    @Test
    void updateUserProfile_shouldUpdateProfileSuccessfully() {
        Long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");
        UpdateUserResponse updateResponse = new UpdateUserResponse(
                1L, "newusername", false, 100L, 10, 5, Instant.now()
        );

        when(authenticationService.getCurrentUserId()).thenReturn(1L);
        when(userService.updateUserProfile(userId, request)).thenReturn(updateResponse);

        ResponseEntity<UpdateUserResponse> response = userController.updateUserProfile(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(1L);
        assertThat(response.getBody().username()).isEqualTo("newusername");
        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, times(1)).updateUserProfile(userId, request);
    }

    @Test
    void updateUserProfile_shouldThrowForbiddenExceptionWhenNotOwner() {
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        when(authenticationService.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> userController.updateUserProfile(userId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can only update your own profile");

        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, never()).updateUserProfile(anyLong(), any(UpdateUserRequest.class));
    }

    @Test
    void updateUserProfile_shouldThrowUserNotFoundException() {
        Long userId = 999L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        when(authenticationService.getCurrentUserId()).thenReturn(999L);
        when(userService.updateUserProfile(userId, request))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThatThrownBy(() -> userController.updateUserProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(authenticationService, times(1)).getCurrentUserId();
        verify(userService, times(1)).updateUserProfile(userId, request);
    }
}

