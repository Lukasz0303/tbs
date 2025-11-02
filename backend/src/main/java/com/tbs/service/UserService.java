package com.tbs.service;

import com.tbs.dto.auth.UserProfileResponse;
import com.tbs.exception.UserNotFoundException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public UserService(UserRepository userRepository, AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    public UserProfileResponse getCurrentUserProfile() {
        Long userId = authenticationService.getCurrentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return mapToUserProfileResponse(user);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getIsGuest(),
                user.getTotalPoints(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getCreatedAt(),
                user.getLastSeenAt()
        );
    }
}

