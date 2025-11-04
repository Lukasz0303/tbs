package com.tbs.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.tbs.exception.UnauthorizedException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new com.tbs.exception.UnauthorizedException("Authentication principal is null");
        }
        
        String principalString = principal.toString();
        try {
            return Long.parseLong(principalString);
        } catch (NumberFormatException e) {
            throw new com.tbs.exception.UnauthorizedException("Authentication failed");
        }
    }
}

