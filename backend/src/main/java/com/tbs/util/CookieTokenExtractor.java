package com.tbs.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;

public final class CookieTokenExtractor {
    
    private static final String AUTH_TOKEN_COOKIE_NAME = "authToken";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private CookieTokenExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    @Nullable
    public static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (AUTH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                String token = cookie.getValue();
                if (token != null && !token.trim().isEmpty()) {
                    return token.trim();
                }
            }
        }
        
        return null;
    }
}

