package com.tbs.dto.auth;

import com.tbs.dto.common.MessageResponse;

public record LogoutResponse(String message) implements MessageResponse {}

