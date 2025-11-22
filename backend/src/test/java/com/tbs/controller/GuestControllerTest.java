package com.tbs.controller;

import com.tbs.dto.guest.GuestRequest;
import com.tbs.dto.guest.GuestResponse;
import com.tbs.service.GuestService;
import com.tbs.service.IpAddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestControllerTest {

    @Mock
    private GuestService guestService;

    @Mock
    private IpAddressService ipAddressService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private GuestController guestController;

    private GuestResponse guestResponse;
    private String testIpAddress = "192.168.1.1";
    private String testToken = "test-jwt-token";

    @BeforeEach
    void setUp() {
        guestResponse = new GuestResponse(
                1L,
                true,
                1,
                0L,
                0,
                0,
                Instant.now()
        );
    }

    @Test
    void createOrGetGuest_shouldReturnCreated_whenNewGuest() {
        GuestResponse newGuestResponse = new GuestResponse(
                1L,
                true,
                1,
                0L,
                0,
                0,
                Instant.now()
        );

        when(ipAddressService.extractIpAddress(any(HttpServletRequest.class), anyString()))
                .thenReturn(testIpAddress);
        when(guestService.findOrCreateGuest(testIpAddress)).thenReturn(newGuestResponse);
        when(guestService.generateTokenForGuest(1L)).thenReturn(testToken);

        ResponseEntity<GuestResponse> response = guestController.createOrGetGuest(null, httpServletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        GuestResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(1L);
    }

    @Test
    void createOrGetGuest_shouldReturnOk_whenExistingGuest() {
        GuestResponse existingGuestResponse = new GuestResponse(
                1L,
                true,
                1,
                100L,
                5,
                3,
                Instant.now().minusSeconds(10)
        );

        when(ipAddressService.extractIpAddress(any(HttpServletRequest.class), anyString()))
                .thenReturn(testIpAddress);
        when(guestService.findOrCreateGuest(testIpAddress)).thenReturn(existingGuestResponse);
        when(guestService.generateTokenForGuest(1L)).thenReturn(testToken);

        ResponseEntity<GuestResponse> response = guestController.createOrGetGuest(null, httpServletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        GuestResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(1L);
    }

    @Test
    void createOrGetGuest_shouldUseProvidedIp_whenGuestRequestProvided() {
        GuestRequest guestRequest = new GuestRequest("10.0.0.1");

        when(ipAddressService.extractIpAddress(any(HttpServletRequest.class), eq("10.0.0.1")))
                .thenReturn("10.0.0.1");
        when(guestService.findOrCreateGuest("10.0.0.1")).thenReturn(guestResponse);

        when(guestService.generateTokenForGuest(1L)).thenReturn(testToken);

        ResponseEntity<GuestResponse> response = guestController.createOrGetGuest(guestRequest, httpServletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        GuestResponse body = response.getBody();
        assertThat(body).isNotNull();
        verify(ipAddressService).extractIpAddress(httpServletRequest, "10.0.0.1");
        verify(guestService).findOrCreateGuest("10.0.0.1");
    }

    @Test
    void createOrGetGuest_shouldExtractIpFromRequest_whenGuestRequestIsNull() {
        when(ipAddressService.extractIpAddress(any(HttpServletRequest.class), isNull()))
                .thenReturn(testIpAddress);
        when(guestService.findOrCreateGuest(testIpAddress)).thenReturn(guestResponse);
        when(guestService.generateTokenForGuest(1L)).thenReturn(testToken);

        ResponseEntity<GuestResponse> response = guestController.createOrGetGuest(null, httpServletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(ipAddressService).extractIpAddress(httpServletRequest, null);
        verify(guestService).findOrCreateGuest(testIpAddress);
    }

    @Test
    void createOrGetGuest_shouldExtractIpFromRequest_whenGuestRequestIpIsNull() {
        GuestRequest guestRequest = new GuestRequest(null);

        when(ipAddressService.extractIpAddress(any(HttpServletRequest.class), isNull()))
                .thenReturn(testIpAddress);
        when(guestService.findOrCreateGuest(testIpAddress)).thenReturn(guestResponse);
        when(guestService.generateTokenForGuest(1L)).thenReturn(testToken);

        ResponseEntity<GuestResponse> response = guestController.createOrGetGuest(guestRequest, httpServletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(ipAddressService).extractIpAddress(httpServletRequest, null);
    }
}

