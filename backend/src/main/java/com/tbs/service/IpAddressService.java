package com.tbs.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class IpAddressService {

    private static final int MAX_IP_ADDRESS_LENGTH = 45;

    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest cannot be null");
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            throw new IllegalStateException("Unable to determine client IP address");
        }

        return remoteAddr;
    }

    public String extractIpAddress(HttpServletRequest request, String providedIp) {
        if (providedIp != null && !providedIp.trim().isEmpty()) {
            String trimmed = providedIp.trim();
            if (trimmed.length() > MAX_IP_ADDRESS_LENGTH) {
                throw new IllegalArgumentException("IP address too long (max " + MAX_IP_ADDRESS_LENGTH + " characters)");
            }
            return trimmed;
        }
        return getClientIpAddress(request);
    }

    public boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ip.trim());
            return inetAddress instanceof java.net.Inet4Address || inetAddress instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}

