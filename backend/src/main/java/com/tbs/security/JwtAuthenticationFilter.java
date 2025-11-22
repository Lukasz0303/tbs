package com.tbs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tbs.util.CookieTokenExtractor;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        if (requestPath == null) {
            return false;
        }
        
        return requestPath.startsWith("/ws/") 
            || requestPath.startsWith("/api/ws/")
            || requestPath.equals("/api/v1/auth/login")
            || requestPath.equals("/api/v1/auth/register")
            || requestPath.equals("/api/auth/login")
            || requestPath.equals("/api/auth/register")
            || requestPath.startsWith("/swagger-ui")
            || requestPath.startsWith("/v3/api-docs")
            || requestPath.startsWith("/actuator/health")
            || requestPath.startsWith("/actuator/info");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);
            if (token != null && validateAndSetAuthentication(token)) {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
        }
        
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        return CookieTokenExtractor.extractToken(request);
    }

    private boolean validateAndSetAuthentication(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Empty token in Authorization header");
            return false;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid JWT token. Token length: {}, Token preview: {}...", token.length(), token.length() > 20 ? token.substring(0, 20) : token);
            return false;
        }

        try {
            String tokenId;
            try {
                tokenId = jwtTokenProvider.getTokenId(token);
            } catch (Exception e) {
                log.warn("Failed to extract token ID from token: {}", e.getMessage());
                return false;
            }
            
            if (tokenBlacklistService.isBlacklisted(tokenId)) {
                log.warn("Attempted access with blacklisted token: {}", tokenId);
                return false;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            log.debug("Successfully authenticated user: {}", userId);
            setAuthentication(userId);
            return true;
        } catch (Exception e) {
            log.warn("Failed to process JWT token: {}", e.getMessage(), e);
            return false;
        }
    }

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

