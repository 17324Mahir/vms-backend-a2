package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.exception.InvalidCredentialsException;
import com.starkgroup.vms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthSecurityTest {

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserRepository userRepository;
    private AuthService authService;

    private static final String TEST_EMAIL = "learner@example.test";
    private static final String TEST_RAW_PASSWORD = "CorrectHorseBatteryStaple@123";
    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-32-bytes-long-for-hmac!";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        String passwordHash = passwordEncoder.encode(TEST_RAW_PASSWORD);

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationSeconds", 900L);
        ReflectionTestUtils.setField(jwtService, "issuer", "stark-vms-backend");
        ReflectionTestUtils.setField(jwtService, "audience", "stark-vms-frontend");
        jwtService.init();

        userRepository = Mockito.mock(UserRepository.class);
        final User mockUser = new User("Learner", TEST_EMAIL, passwordHash, "USER");
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(userRepository.findByEmailIgnoreCase("LEARNER@EXAMPLE.TEST")).thenReturn(Optional.of(mockUser));

        authService = new AuthServiceImpl(passwordEncoder, jwtService, userRepository);
    }

    @Test
    @DisplayName("Valid credentials authenticate successfully and issue JWT token")
    void login_withValidCredentials_succeeds() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_RAW_PASSWORD);
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertFalse(response.accessToken().isEmpty());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertNotNull(response.user());
        assertEquals(TEST_EMAIL, response.user().email());
    }

    @Test
    @DisplayName("Invalid password throws InvalidCredentialsException with generic error")
    void login_withInvalidPassword_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword@999");
        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Email or password is incorrect.", ex.getMessage());
    }

    @Test
    @DisplayName("Unknown account throws InvalidCredentialsException with generic error")
    void login_withUnknownEmail_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("unknown.user@example.test", TEST_RAW_PASSWORD);
        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Email or password is incorrect.", ex.getMessage());
    }

    @Test
    @DisplayName("Case-insensitive email matches configured account")
    void login_withDifferentCaseEmail_succeeds() {
        LoginRequest request = new LoginRequest("LEARNER@EXAMPLE.TEST", TEST_RAW_PASSWORD);
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.user().email());
    }

    @Test
    @DisplayName("JwtService rejects secret keys shorter than 32 bytes")
    void jwtService_rejectsShortSecretKey() {
        JwtService shortKeyService = new JwtService();
        ReflectionTestUtils.setField(shortKeyService, "jwtSecret", "too-short-secret");
        ReflectionTestUtils.setField(shortKeyService, "expirationSeconds", 900L);
        ReflectionTestUtils.setField(shortKeyService, "issuer", "stark-vms-backend");
        ReflectionTestUtils.setField(shortKeyService, "audience", "stark-vms-frontend");

        assertThrows(IllegalStateException.class, shortKeyService::init);
    }

    @Test
    @DisplayName("JwtService rejects null or blank secret key")
    void jwtService_rejectsBlankSecretKey() {
        JwtService blankKeyService = new JwtService();
        ReflectionTestUtils.setField(blankKeyService, "jwtSecret", "   ");

        assertThrows(IllegalStateException.class, blankKeyService::init);
    }
}
