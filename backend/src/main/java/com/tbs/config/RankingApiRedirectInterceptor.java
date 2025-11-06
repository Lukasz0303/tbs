package com.tbs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RankingApiRedirectInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/rankings") && !path.startsWith("/api/v1/rankings")) {
            String newPath = path.replace("/api/rankings", "/api/v1/rankings");
            String queryString = request.getQueryString();
            if (queryString != null) {
                newPath += "?" + queryString;
            }
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", newPath);
            return false;
        }
        return true;
    }
}

