package com.tbs.service;

import com.tbs.dto.user.LastSeenResponse;
import com.tbs.dto.user.UpdateAvatarRequest;
import com.tbs.dto.user.UpdateUserRequest;
import com.tbs.dto.user.UpdateUserResponse;
import com.tbs.dto.user.UserProfileResponse;
import com.tbs.exception.ConflictException;
import com.tbs.exception.ForbiddenException;
import com.tbs.exception.UserNotFoundException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public UserService(UserRepository userRepository, AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    public com.tbs.dto.auth.UserProfileResponse getCurrentUserProfile() {
        Long userId = authenticationService.getCurrentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return mapToAuthUserProfileResponse(user);
    }

    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getIsGuest() && (currentUserId == null || !userId.equals(currentUserId))) {
            throw new ForbiddenException("Access denied to guest profile");
        }

        return mapToUserProfileResponse(user);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public LastSeenResponse updateLastSeen(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Instant now = Instant.now();
        user.setLastSeenAt(now);
        userRepository.save(user);

        return new LastSeenResponse("Last seen updated successfully", now);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UpdateUserResponse updateUserProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            user.setUsername(request.username());
        }

        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        try {
            userRepository.save(user);
            log.debug("User profile updated: userId={}, username={}, avatar={}", 
                    userId, request.username(), request.avatar());
        } catch (DataIntegrityViolationException e) {
            log.warn("Data integrity violation during profile update: userId={}", userId, e);
            throw new ConflictException("Username already exists");
        }

        return mapToUpdateUserResponse(user);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UpdateUserResponse updateUserAvatar(Long userId, UpdateAvatarRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setAvatar(request.avatar());
        userRepository.save(user);
        
        log.debug("User avatar updated: userId={}, avatar={}", userId, request.avatar());

        return mapToUpdateUserResponse(user);
    }

    private Integer getAvatarOrDefault(User user) {
        return Optional.ofNullable(user.getAvatar()).orElse(1);
    }

    private com.tbs.dto.auth.UserProfileResponse mapToAuthUserProfileResponse(User user) {
        return new com.tbs.dto.auth.UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsGuest(),
                getAvatarOrDefault(user),
                user.getTotalPoints(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getCreatedAt(),
                user.getLastSeenAt()
        );
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getIsGuest(),
                getAvatarOrDefault(user),
                user.getTotalPoints(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getCreatedAt()
        );
    }

    private UpdateUserResponse mapToUpdateUserResponse(User user) {
        return new UpdateUserResponse(
                user.getId(),
                user.getUsername(),
                user.getIsGuest(),
                getAvatarOrDefault(user),
                user.getTotalPoints(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getUpdatedAt()
        );
    }
}

