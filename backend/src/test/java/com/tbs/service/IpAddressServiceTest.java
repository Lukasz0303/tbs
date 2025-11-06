package com.tbs.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpAddressServiceTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private IpAddressService ipAddressService;

    @Test
    void getClientIpAddress_shouldReturnXForwardedFor_whenHeaderPresent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        String ip = ipAddressService.getClientIpAddress(request);

        assertThat(ip).isEqualTo("192.168.1.1");
    }

    @Test
    void getClientIpAddress_shouldReturnXRealIp_whenXForwardedForNotPresent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.2");

        String ip = ipAddressService.getClientIpAddress(request);

        assertThat(ip).isEqualTo("192.168.1.2");
    }

    @Test
    void getClientIpAddress_shouldReturnRemoteAddr_whenHeadersNotPresent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");

        String ip = ipAddressService.getClientIpAddress(request);

        assertThat(ip).isEqualTo("192.168.1.3");
    }

    @Test
    void getClientIpAddress_shouldThrowException_whenRequestIsNull() {
        assertThatThrownBy(() -> ipAddressService.getClientIpAddress(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HttpServletRequest cannot be null");
    }

    @Test
    void getClientIpAddress_shouldThrowException_whenRemoteAddrIsNull() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertThatThrownBy(() -> ipAddressService.getClientIpAddress(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to determine client IP address");
    }

    @Test
    void extractIpAddress_shouldReturnProvidedIp_whenProvided() {
        String providedIp = "192.168.1.1";

        String ip = ipAddressService.extractIpAddress(request, providedIp);

        assertThat(ip).isEqualTo(providedIp);
        verify(request, never()).getHeader(anyString());
    }

    @Test
    void extractIpAddress_shouldReturnClientIp_whenProvidedIpIsNull() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");

        String ip = ipAddressService.extractIpAddress(request, null);

        assertThat(ip).isEqualTo("192.168.1.2");
    }

    @Test
    void extractIpAddress_shouldThrowException_whenIpTooLong() {
        String longIp = "a".repeat(46);

        assertThatThrownBy(() -> ipAddressService.extractIpAddress(request, longIp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IP address too long");
    }

    @Test
    void isValidIpAddress_shouldReturnTrue_forValidIPv4() {
        assertThat(ipAddressService.isValidIpAddress("192.168.1.1")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("127.0.0.1")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("10.0.0.1")).isTrue();
    }

    @Test
    void isValidIpAddress_shouldReturnTrue_forValidIPv6() {
        assertThat(ipAddressService.isValidIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334")).isTrue();
        assertThat(ipAddressService.isValidIpAddress("::1")).isTrue();
    }

    @Test
    void isValidIpAddress_shouldReturnFalse_forInvalidIp() {
        assertThat(ipAddressService.isValidIpAddress("invalid")).isFalse();
        assertThat(ipAddressService.isValidIpAddress("999.999.999.999")).isFalse();
        assertThat(ipAddressService.isValidIpAddress("")).isFalse();
    }

    @Test
    void isValidIpAddress_shouldReturnFalse_forNull() {
        assertThat(ipAddressService.isValidIpAddress(null)).isFalse();
    }

    @Test
    void isValidIpAddress_shouldHandleWhitespace() {
        assertThat(ipAddressService.isValidIpAddress("  192.168.1.1  ")).isTrue();
    }
}

