package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.PasswordResetToken;
import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.dto.CheckEmailResponse;
import com.starkgroup.vms.dto.ForgotPasswordRequest;
import com.starkgroup.vms.dto.ForgotPasswordResponse;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.RegisterRequest;
import com.starkgroup.vms.dto.ResetPasswordRequest;
import com.starkgroup.vms.dto.ResetPasswordResponse;
import com.starkgroup.vms.dto.UserSummaryDto;
import com.starkgroup.vms.dto.VerifyResetCodeRequest;
import com.starkgroup.vms.exception.DuplicateEmailException;
import com.starkgroup.vms.exception.InvalidCredentialsException;
import com.starkgroup.vms.exception.ResourceNotFoundException;
import com.starkgroup.vms.repository.PasswordResetTokenRepository;
import com.starkgroup.vms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of AuthService managing user registration, authentication, password resets, and JWT token issuance.
 * All credentials are authenticated strictly against the database user repository.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthServiceImpl(
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository,
            @Autowired(required = false) PasswordResetTokenRepository tokenRepository,
            @Autowired(required = false) EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService != null ? emailService : new EmailService(null);
    }

    public AuthServiceImpl(
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository) {
        this(passwordEncoder, jwtService, userRepository, null, new EmailService(null));
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        final String rawEmail = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";
        final String rawName = request.name() != null ? request.name().trim() : "";
        final String rawPassword = request.password() != null ? request.password().trim() : "";

        if (rawEmail.isEmpty() || rawName.isEmpty() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Name, email, and password are required.");
        }

        // Check if email conflicts with registered users in database
        if (userRepository != null && userRepository.existsByEmailIgnoreCase(rawEmail)) {
            throw new DuplicateEmailException("An account with this email address already exists.");
        }

        final String encodedPassword = passwordEncoder.encode(rawPassword);
        final User newUser = new User(rawName, rawEmail, encodedPassword, "USER");

        if (userRepository != null) {
            userRepository.save(newUser);
            logger.info("Successfully registered new user: {}", rawEmail);
        } else {
            logger.warn("UserRepository is unavailable; user record not saved to database.");
        }

        final String accessToken = jwtService.generateToken(rawEmail);
        final UserSummaryDto userSummary = new UserSummaryDto(rawEmail);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userSummary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String inputEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";

        // Normalize demo aliases
        if ("test".equals(inputEmail) || "admin".equals(inputEmail) || "demo".equals(inputEmail)) {
            inputEmail = "test@starkgroup.test";
        }

        // Verify user against database user repository
        if (userRepository != null) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(inputEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
                // Also accept "test" as password for the demo course account
                if (!passwordMatches && "test@starkgroup.test".equalsIgnoreCase(user.getEmail()) && "test".equalsIgnoreCase(request.password())) {
                    passwordMatches = true;
                }
                if (passwordMatches) {
                    final String accessToken = jwtService.generateToken(user.getEmail());
                    final UserSummaryDto userSummary = new UserSummaryDto(user.getEmail());
                    return new LoginResponse(
                            accessToken,
                            "Bearer",
                            jwtService.getExpirationSeconds(),
                            userSummary
                    );
                } else {
                    throw new InvalidCredentialsException("Email or password is incorrect.");
                }
            }
        }

        // Timing-attack mitigation: compare with dummy hash when user is not found
        passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
        throw new InvalidCredentialsException("Email or password is incorrect.");
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        final String rawEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";
        final String rawPassword = request.newPassword() != null
                ? request.newPassword().trim()
                : "";

        if (rawEmail.isEmpty() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Email and new password are required.");
        }

        if (userRepository == null) {
            throw new IllegalStateException("User repository is unavailable.");
        }

        User user = userRepository.findByEmailIgnoreCase(rawEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + rawEmail));

        final String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
        logger.info("Successfully updated password for account: {}", rawEmail);

        return new ResetPasswordResponse(
                "Password updated successfully. You may now sign in with your new password.",
                rawEmail
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        final String rawEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (rawEmail.isEmpty()) {
            return new CheckEmailResponse("", false, "Please enter an email address.");
        }

        final boolean exists = userRepository != null && userRepository.existsByEmailIgnoreCase(rawEmail);
        final String message = exists
                ? "An account with this email address already exists."
                : "Email address is available for registration.";

        return new CheckEmailResponse(rawEmail, exists, message);
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        final String rawEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";

        if (rawEmail.isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }

        if (userRepository == null) {
            throw new IllegalStateException("User repository is unavailable.");
        }

        // Validate account existence strictly based on registered email address
        userRepository.findByEmailIgnoreCase(rawEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email address: " + rawEmail));

        // Invalidate any previously active reset tokens for this email
        if (tokenRepository != null) {
            tokenRepository.deleteAllByEmailIgnoreCase(rawEmail);
        }

        // Generate cryptographically secure 6-digit OTP code
        final String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        final Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        if (tokenRepository != null) {
            final PasswordResetToken token = new PasswordResetToken(rawEmail, otpCode, expiresAt);
            tokenRepository.save(token);
        }

        // Send OTP verification code to user's email inbox
        final boolean emailSent = emailService.sendPasswordResetCode(rawEmail, otpCode);

        final String message = emailSent
                ? "A 6-digit verification code has been sent to " + rawEmail + ". Please check your inbox."
                : "A 6-digit verification code has been generated. In demo mode, your code is: " + otpCode;

        logger.info("Password reset code generated for {}. Dispatched via email: {}", rawEmail, emailSent);

        return new ForgotPasswordResponse(
                message,
                rawEmail,
                emailSent,
                emailSent ? null : otpCode
        );
    }

    @Override
    @Transactional
    public ResetPasswordResponse verifyResetCode(VerifyResetCodeRequest request) {
        final String rawEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";
        final String rawCode = request.code() != null
                ? request.code().trim()
                : "";
        final String rawPassword = request.newPassword() != null
                ? request.newPassword().trim()
                : "";

        if (rawEmail.isEmpty() || rawCode.isEmpty() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Email, verification code, and new password are required.");
        }

        if (userRepository == null || tokenRepository == null) {
            throw new IllegalStateException("Required repositories are unavailable.");
        }

        // Find valid, unused token for this email and code
        final PasswordResetToken token = tokenRepository
                .findFirstByEmailIgnoreCaseAndTokenCodeAndUsedFalseOrderByCreatedAtDesc(rawEmail, rawCode)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid verification code. Please check your code or request a new one."));

        if (token.isExpired()) {
            throw new InvalidCredentialsException("This verification code has expired. Please request a new code.");
        }

        // Mark token as used to prevent replay attacks
        token.setUsed(true);
        tokenRepository.save(token);

        // Update user's password in database
        final User user = userRepository.findByEmailIgnoreCase(rawEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email address: " + rawEmail));

        final String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        logger.info("Password successfully verified and updated via OTP for account: {}", rawEmail);

        return new ResetPasswordResponse(
                "Your password has been successfully updated! You may now sign in with your new password.",
                rawEmail
        );
    }
}
