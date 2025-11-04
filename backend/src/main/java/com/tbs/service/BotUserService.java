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
    private static final Long BOT_USER_ID = 0L;
    private static final String BOT_USERNAME = "Bot";

    private final UserRepository userRepository;

    public BotUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getBotUser() {
        return userRepository.findById(BOT_USER_ID)
                .orElseGet(() -> {
                    log.warn("Bot user not found in database, creating default bot user");
                    return createDefaultBotUser();
                });
    }

    @Transactional
    private User createDefaultBotUser() {
        User botUser = new User();
        botUser.setId(Long.valueOf(BOT_USER_ID));
        botUser.setUsername(BOT_USERNAME);
        botUser.setEmail(null);
        botUser.setPasswordHash(null);
        botUser.setIsGuest(false);
        botUser.setIpAddress(null);
        botUser.setTotalPoints(0L);
        botUser.setGamesPlayed(0);
        botUser.setGamesWon(0);
        try {
            return userRepository.save(botUser);
        } catch (Exception e) {
            log.error("Failed to create bot user, returning user with ID=0", e);
            return botUser;
        }
    }
}

