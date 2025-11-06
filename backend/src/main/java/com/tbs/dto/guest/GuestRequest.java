package com.tbs.dto.guest;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestRequest(
    @Pattern(
        regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$",
        message = "Invalid IP address format (IPv4 or IPv6)"
    )
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    String ipAddress
) {}

