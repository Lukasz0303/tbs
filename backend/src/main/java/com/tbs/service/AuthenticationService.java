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
        
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new com.tbs.exception.UnauthorizedException("Invalid user ID in authentication");
            }
        }
        
        throw new com.tbs.exception.UnauthorizedException("Unexpected principal type: " + principal.getClass().getName());
    }

    public Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
}

