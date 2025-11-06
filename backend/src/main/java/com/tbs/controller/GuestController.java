package com.tbs.controller;

import com.tbs.dto.guest.GuestRequest;
import com.tbs.dto.guest.GuestResponse;
import com.tbs.service.GuestService;
import com.tbs.service.IpAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/guests")
@Tag(name = "Guest", description = "API endpoints for guest user management")
public class GuestController {

    private final GuestService guestService;
    private final IpAddressService ipAddressService;
    private final HttpServletRequest httpServletRequest;
    private final int newUserThresholdSeconds;

    public GuestController(
            GuestService guestService,
            IpAddressService ipAddressService,
            HttpServletRequest httpServletRequest,
            @Value("${app.guest.new-user-threshold-seconds:2}") int newUserThresholdSeconds
    ) {
        this.guestService = guestService;
        this.ipAddressService = ipAddressService;
        this.httpServletRequest = httpServletRequest;
        this.newUserThresholdSeconds = newUserThresholdSeconds;
    }

    @PostMapping
    @Operation(
            summary = "Create or get guest profile",
            description = "Creates a new guest profile or returns existing one based on IP address. " +
                    "If ipAddress is provided in request body, it will be used. Otherwise, IP address " +
                    "will be extracted from request headers (X-Forwarded-For, X-Real-IP) or remote address. " +
                    "Returns JWT token that can be used to access other authenticated endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Guest profile already exists",
                    content = @Content(schema = @Schema(implementation = GuestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "New guest profile created",
                    content = @Content(schema = @Schema(implementation = GuestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid IP address format or unable to determine IP address"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<GuestResponse> createOrGetGuest(@Valid @RequestBody(required = false) GuestRequest guestRequest) {
        String ipAddress = ipAddressService.extractIpAddress(
                httpServletRequest,
                guestRequest != null ? guestRequest.ipAddress() : null
        );

        GuestResponse response = guestService.findOrCreateGuest(ipAddress);

        boolean isNewlyCreated = response.createdAt().isAfter(Instant.now().minusSeconds(newUserThresholdSeconds));
        HttpStatus status = isNewlyCreated ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }
}

