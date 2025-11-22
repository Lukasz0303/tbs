package com.tbs.service;

import com.tbs.dto.guest.GuestResponse;
import com.tbs.exception.BadRequestException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class GuestService {

    private static final Logger log = LoggerFactory.getLogger(GuestService.class);

    private final UserRepository userRepository;
    private final IpAddressService ipAddressService;
    private final JwtTokenProvider jwtTokenProvider;

    public GuestService(
            UserRepository userRepository,
            IpAddressService ipAddressService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.ipAddressService = ipAddressService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public GuestResponse findOrCreateGuest(String ipAddress) {
        if (!ipAddressService.isValidIpAddress(ipAddress)) {
            throw new BadRequestException("Invalid IP address format");
        }

        Optional<User> existingGuest = userRepository.findByIpAddressAndIsGuest(ipAddress, true);

        if (existingGuest.isPresent()) {
            log.info("Found existing guest profile for IP: {}", maskIpAddress(ipAddress));
            return mapToGuestResponse(existingGuest.get());
        }

        try {
            User newGuest = createGuestUser(ipAddress);
            User savedGuest = userRepository.save(newGuest);
            log.info("Created new guest profile for IP: {}", maskIpAddress(ipAddress));
            return mapToGuestResponse(savedGuest);
        } catch (DataIntegrityViolationException e) {
            log.debug("Guest already exists (race condition), fetching existing: {}", maskIpAddress(ipAddress));
            Optional<User> guest = userRepository.findByIpAddressAndIsGuest(ipAddress, true);
            if (guest.isPresent()) {
                return mapToGuestResponse(guest.get());
            }
            throw new BadRequestException("Unable to create or retrieve guest profile");
        }
    }

    private User createGuestUser(String ipAddress) {
        User guest = new User();
        guest.setIsGuest(true);
        guest.setIpAddress(ipAddress);
        guest.setAvatar(1);
        guest.setTotalPoints(0L);
        guest.setGamesPlayed(0);
        guest.setGamesWon(0);
        return guest;
    }

    private GuestResponse mapToGuestResponse(User user) {
        if (user.getId() == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        return new GuestResponse(
                user.getId(),
                true,
                Optional.ofNullable(user.getAvatar()).orElse(1),
                Optional.ofNullable(user.getTotalPoints()).orElse(0L),
                Optional.ofNullable(user.getGamesPlayed()).orElse(0),
                Optional.ofNullable(user.getGamesWon()).orElse(0),
                Optional.ofNullable(user.getCreatedAt()).orElseGet(Instant::now)
        );
    }

    public String generateTokenForGuest(Long userId) {
        return jwtTokenProvider.generateToken(userId);
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        int lastDotIndex = ipAddress.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return ipAddress.substring(0, lastDotIndex + 1) + "xxx";
        }
        return "xxx";
    }
}

