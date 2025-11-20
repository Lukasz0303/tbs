adpackage com.tbs.service;

import com.tbs.dto.auth.LoginRequest;
import com.tbs.dto.auth.LoginResponse;
import com.tbs.dto.auth.RegisterRequest;
import com.tbs.dto.auth.RegisterResponse;
import com.tbs.exception.UnauthorizedException;
import com.tbs.model.User;
import com.tbs.repository.UserRepository;
import com.tbs.security.JwtTokenProvider;
import com.tbs.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("$2a$10$encodedHash");
        testUser.setIsGuest(false);
        testUser.setTotalPoints(100L);
        testUser.setGamesPlayed(10);
        testUser.setGamesWon(5);
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        String validToken = "valid-jwt-token";
        
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtTokenProvider.generateToken(1L))
                .thenReturn(validToken);

        LoginResponse response = authService.login(
                new LoginRequest("test@example.com", "password123")
        );

        assertThat(response.authToken()).isEqualTo(validToken);
        assertThat(response.userId()).isEqualTo("1");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.isGuest()).isFalse();
        assertThat(response.totalPoints()).isEqualTo(100L);
        assertThat(response.gamesPlayed()).isEqualTo(10);
        assertThat(response.gamesWon()).isEqualTo(5);
    }

    @Test
    void login_shouldThrowUnauthorizedForNonExistentEmail() {
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nonexistent@example.com", "password123")
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void login_shouldThrowUnauthorizedForInvalidPassword() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test@example.com", "wrongpassword")
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void login_shouldThrowUnauthorizedForNullPasswordHash() {
        testUser.setPasswordHash(null);
        
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test@example.com", "password123")
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        String validToken = "valid-jwt-token";
        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setEmail("newuser@example.com");
        savedUser.setUsername("newuser");
        savedUser.setIsGuest(false);
        savedUser.setTotalPoints(0L);
        savedUser.setGamesPlayed(0);
        savedUser.setGamesWon(0);

        when(userRepository.existsByEmail("newuser@example.com"))
                .thenReturn(false);
        when(userRepository.existsByUsername("newuser"))
                .thenReturn(false);
        when(passwordEncoder.encode("securePass123"))
                .thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(2L))
                .thenReturn(validToken);

        RegisterResponse response = authService.register(
                new RegisterRequest("newuser@example.com", "securePass123", "newuser", null)
        );

        assertThat(response.authToken()).isEqualTo(validToken);
        assertThat(response.userId()).isEqualTo("2");
        assertThat(response.email()).isEqualTo("newuser@example.com");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.isGuest()).isFalse();
        assertThat(response.totalPoints()).isEqualTo(0L);
        assertThat(response.gamesPlayed()).isEqualTo(0);
        assertThat(response.gamesWon()).isEqualTo(0);

        verify(passwordEncoder).encode("securePass123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowBadRequestForDuplicateEmail() {
        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("existing@example.com", "password123", "username", null)
        ))
                .isInstanceOf(com.tbs.exception.BadRequestException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowBadRequestForDuplicateUsername() {
        when(userRepository.existsByEmail("newemail@example.com"))
                .thenReturn(false);
        when(userRepository.existsByUsername("existinguser"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("newemail@example.com", "password123", "existinguser", null)
        ))
                .isInstanceOf(com.tbs.exception.BadRequestException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }
}

