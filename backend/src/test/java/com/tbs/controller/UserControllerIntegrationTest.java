package com.tbs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbs.dto.user.UpdateUserRequest;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User guestUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setIsGuest(false);
        testUser.setTotalPoints(100L);
        testUser.setGamesPlayed(10);
        testUser.setGamesWon(5);
        testUser.setCreatedAt(Instant.now());
        testUser = userRepository.save(testUser);

        guestUser = new User();
        guestUser.setUsername(null);
        guestUser.setIsGuest(true);
        guestUser.setTotalPoints(50L);
        guestUser.setGamesPlayed(5);
        guestUser.setGamesWon(2);
        guestUser.setCreatedAt(Instant.now());
        guestUser = userRepository.save(guestUser);

        authToken = jwtTokenProvider.generateToken(testUser.getId());
    }

    @Test
    void getUserProfile_shouldReturnProfileForRegisteredUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.isGuest").value(false))
                .andExpect(jsonPath("$.totalPoints").value(100))
                .andExpect(jsonPath("$.gamesPlayed").value(10))
                .andExpect(jsonPath("$.gamesWon").value(5))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getUserProfile_shouldReturnProfileForGuestUserWhenOwner() throws Exception {
        String guestToken = jwtTokenProvider.generateToken(guestUser.getId());

        mockMvc.perform(get("/api/v1/users/{userId}", guestUser.getId())
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(guestUser.getId()))
                .andExpect(jsonPath("$.username").isEmpty())
                .andExpect(jsonPath("$.isGuest").value(true));
    }

    @Test
    void getUserProfile_shouldReturn403ForGuestUserWhenNotOwner() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", guestUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("Access denied to guest profile"));
    }

    @Test
    void getUserProfile_shouldReturn404ForNonExistentUser() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/v1/users/{userId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("User not found"));
    }

    @Test
    void updateLastSeen_shouldUpdateTimestampSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/last-seen", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Last seen updated successfully"))
                .andExpect(jsonPath("$.lastSeenAt").exists());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getLastSeenAt()).isNotNull();
    }

    @Test
    void updateLastSeen_shouldReturn403WhenNotOwner() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setIsGuest(false);
        otherUser = userRepository.save(otherUser);
        String otherToken = jwtTokenProvider.generateToken(otherUser.getId());

        mockMvc.perform(post("/api/v1/users/{userId}/last-seen", testUser.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("You can only update your own last seen timestamp"));
    }

    @Test
    void updateLastSeen_shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/last-seen", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLastSeen_shouldReturn404ForNonExistentUser() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(post("/api/v1/users/{userId}/last-seen", nonExistentId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void updateUserProfile_shouldUpdateUsernameSuccessfully() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.updatedAt").exists());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("newusername");
    }

    @Test
    void updateUserProfile_shouldReturn400ForInvalidUsername() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("ab");

        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateUserProfile_shouldReturn403WhenNotOwner() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setIsGuest(false);
        otherUser = userRepository.save(otherUser);
        String otherToken = jwtTokenProvider.generateToken(otherUser.getId());

        UpdateUserRequest request = new UpdateUserRequest("newusername");

        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("You can only update your own profile"));
    }

    @Test
    void updateUserProfile_shouldReturn409WhenUsernameExists() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("existinguser");
        otherUser.setIsGuest(false);
        userRepository.save(otherUser);

        UpdateUserRequest request = new UpdateUserRequest("existinguser");

        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Username already exists"));
    }

    @Test
    void updateUserProfile_shouldReturn404ForNonExistentUser() throws Exception {
        Long nonExistentId = 99999L;
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        mockMvc.perform(put("/api/v1/users/{userId}", nonExistentId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void updateUserProfile_shouldReturn401WhenUnauthenticated() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("newusername");

        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

