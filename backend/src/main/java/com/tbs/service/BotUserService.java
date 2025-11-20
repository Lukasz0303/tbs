package com.tbs.service;

import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BotUserService {

    private static final Logger log = LoggerFactory.getLogger(BotUserService.class);
    private static final String BOT_USERNAME = "Bot";

    private final UserRepository userRepository;

    public BotUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getBotUser() {
        return userRepository.findByUsername(BOT_USERNAME)
                .orElseGet(() -> {
                    log.warn("Bot user not found in database, creating default bot user");
                    return createDefaultBotUser();
                });
    }

    @Transactional
    private User createDefaultBotUser() {
        User botUser = new User();
        botUser.setUsername(BOT_USERNAME);
        botUser.setEmail("bot@system.local");
        botUser.setPasswordHash("$2a$10$botSystemPasswordHashPlaceholder");
        botUser.setIsGuest(false);
        botUser.setAvatar(1);
        botUser.setIpAddress(null);
        botUser.setTotalPoints(0L);
        botUser.setGamesPlayed(0);
        botUser.setGamesWon(0);
        try {
            return userRepository.save(botUser);
        } catch (Exception e) {
            log.error("Failed to create bot user, trying to retrieve existing one", e);
            return userRepository.findByUsername(BOT_USERNAME)
                    .orElseThrow(() -> new IllegalStateException("Failed to create bot user and user not found", e));
        }
    }
}

