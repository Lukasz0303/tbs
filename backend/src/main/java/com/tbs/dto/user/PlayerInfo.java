package com.tbs.dto.user;

public record PlayerInfo(
        long userId,
        String username,
        boolean isGuest
) {}

