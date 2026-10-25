package com.taskmanagesys.auth.service;

import com.taskmanagesys.auth.dto.*;
import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.RefreshToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.common.validation.ValidationUtils;
import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.user.dto.UserDto;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_DAYS = 7;
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailOutboxService emailOutboxService;
    private final RateLimiterService rateLimiterService;
    private final NotificationService notifications;

    public AuthService(
            UserRepository userRepository,
            EmailTokenRepository emailTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            EmailOutboxService emailOutboxService,
            RateLimiterService rateLimiterService,
            NotificationService notifications) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailOutboxService = emailOutboxService;
        this.rateLimiterService = rateLimiterService;
        this.notifications = notifications;
    }

    @Transactional
    public Map<String, String> signUp(SignUpRequest req, String clientIp) {
        rateLimiterService.checkSignUp(clientIp);

        String firstName = ValidationUtils.validateAndNormalizeName(req.getFirstName(), "First name");
        String lastName = ValidationUtils.validateAndNormalizeName(req.getLastName(), "Last name");
        String email = ValidationUtils.validateAndNormalizeEmail(req.getEmail());
        String phone = ValidationUtils.validateAndNormalizePhone(req.getPhone());
        ValidationUtils.validatePassword(req.getPassword(), email);

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            notifications.accountEmail(existingUserOpt.get().getId(), "ACCOUNT_ALREADY_EXISTS", "ACCOUNT_ALREADY_EXISTS", "A sign-in link was requested for your account.", Map.of());
            return Map.of("message", "If your email is eligible, a verification link has been sent. Please check your inbox.");
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(com.taskmanagesys.user.model.Role.EMPLOYEE);
        user.setStatus(UserStatus.PENDING_EMAIL);
        user.setFailedLoginCount(0);
        User savedUser = userRepository.save(user);

        String rawToken = TokenUtils.generateRandomToken();
        String tokenHash = TokenUtils.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        EmailToken emailToken = new EmailToken(savedUser, TokenType.VERIFY_EMAIL, tokenHash, expiresAt);
        emailTokenRepository.save(emailToken);

        notifications.accountEmail(savedUser.getId(), "VERIFY_EMAIL", "VERIFY_EMAIL", "Please verify your email address.", Map.of("token", rawToken));

        return Map.of("message", "Registration successful. Please check your email to verify your address.");
    }

    @Transactional
    public Map<String, String> verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TOKEN_INVALID", "Verification token is required");
        }

        String hash = TokenUtils.hashToken(rawToken.trim());
        EmailToken emailToken = emailTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "TOKEN_INVALID", "Invalid or expired verification link"));

        if (emailToken.getUsedAt() != null || emailToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "This verification link is expired or has already been used");
        }

        User user = emailToken.getUser();
        emailToken.setUsedAt(Instant.now());
        emailTokenRepository.save(emailToken);

        if (emailToken.getType() == TokenType.VERIFY_EMAIL) {
            if (user.getStatus() == UserStatus.PENDING_EMAIL) {
                user.setStatus(UserStatus.PENDING_APPROVAL);
                userRepository.save(user);

                notifications.signupRequest(user);
            }
            return Map.of("message", "Email verified successfully. Your request is now pending Manager approval.");
        } else if (emailToken.getType() == TokenType.CHANGE_EMAIL) {
            String newEmail = emailToken.getNewEmail();
            if (newEmail == null || userRepository.existsByEmail(newEmail)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "New email address is already in use");
            }
            user.setEmail(newEmail);
            userRepository.save(user);
            return Map.of("message", "Email address updated successfully.");
        }

        throw new AppException(HttpStatus.BAD_REQUEST, "TOKEN_INVALID", "Unsupported verification token type");
    }

    @Transactional(noRollbackFor = AppException.class)
    public LoginResponse login(LoginRequest req, HttpServletResponse response) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String password = req.getPassword();

        User user = userRepository.findByEmail(email).orElse(null);

        // Security: always perform password check or dummy to prevent timing analysis
        if (user == null || user.getDeletedAt() != null) {
            passwordEncoder.matches("dummy", DUMMY_PASSWORD_HASH);
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        // BR-8: Lockout check
        if (user.getLockedUntil() != null) {
            if (Instant.now().isBefore(user.getLockedUntil())) {
                long minutesLeft = Math.max(1, Duration.between(Instant.now(), user.getLockedUntil()).toMinutes() + 1);
                throw new AppException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                        "Account is locked due to multiple failed login attempts. Please try again in " + minutesLeft + " minute(s).");
            } else {
                user.setLockedUntil(null);
                user.setFailedLoginCount(0);
                userRepository.save(user);
            }
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            int failed = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failed);
            if (failed >= 5) {
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Reset lock counter on successful password match
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // BR-8: Clear status message for non-ACTIVE accounts
        if (user.getStatus() != UserStatus.ACTIVE) {
            String message = switch (user.getStatus()) {
                case PENDING_EMAIL -> "Please verify your email address to continue.";
                case PENDING_APPROVAL -> "Your account request is waiting for Manager approval.";
                case REJECTED -> "Your account sign-up request was rejected.";
                case DISABLED -> "Your account has been deactivated. Please contact your manager.";
                case INVITED -> "Your invitation has not been completed. Please check your email to set your password.";
                default -> "Account is not active.";
            };
            throw new AppException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", message);
        }

        String accessToken = jwtService.generateAccessToken(user);
        issueRefreshToken(user, response);

        return new LoginResponse(accessToken, UserDto.from(user));
    }

    @Transactional
    public LoginResponse refreshToken(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REQUIRED", "Refresh token is missing");
        }

        String hash = TokenUtils.hashToken(rawRefreshToken.trim());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh token is expired or revoked");
        }

        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", "Account is no longer active");
        }

        // BR-8: Rotate refresh token on every use
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        String newAccessToken = jwtService.generateAccessToken(user);
        issueRefreshToken(user, response);

        return new LoginResponse(newAccessToken, UserDto.from(user));
    }

    @Transactional
    public void logout(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = TokenUtils.hashToken(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
        clearRefreshCookie(response);
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest req, String clientIp) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        rateLimiterService.checkForgotPassword(clientIp, email);

        // BR-9: Always return the exact same generic response
        String genericMessage = "If the account exists, we sent a password reset link to your email.";

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // BR-9: Only ACTIVE accounts receive a working link
            if (user.getStatus() == UserStatus.ACTIVE && user.getDeletedAt() == null) {
                emailTokenRepository.invalidateActiveTokensForUser(user, TokenType.RESET_PASSWORD);

                String rawToken = TokenUtils.generateRandomToken();
                String hash = TokenUtils.hashToken(rawToken);
                Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);

                EmailToken token = new EmailToken(user, TokenType.RESET_PASSWORD, hash, expiresAt);
                emailTokenRepository.save(token);

                notifications.accountEmail(user.getId(), "PASSWORD_RESET", "PASSWORD_RESET", "A password reset link is ready.", Map.of("token", rawToken));
            }
        }

        return Map.of("message", genericMessage);
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TOKEN_REQUIRED", "Reset token is required");
        }

        String hash = TokenUtils.hashToken(req.getToken().trim());
        EmailToken token = emailTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "RESET_LINK_INVALID",
                        "This link is expired or invalid. Please request a new link."));

        if (token.getType() != TokenType.RESET_PASSWORD || token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "RESET_LINK_INVALID",
                    "This link is expired or invalid. Please request a new link.");
        }

        User user = token.getUser();
        ValidationUtils.validatePassword(req.getNewPassword(), user.getEmail());

        if (user.getPasswordHash() != null && passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PASSWORD_SAME",
                    "New password must differ from your current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);

        // BR-9: All refresh tokens revoked
        refreshTokenRepository.revokeAllForUser(user);

        // BR-9: Password changed confirmation email
        notifications.accountEmail(user.getId(), "PASSWORD_CHANGED", "PASSWORD_CHANGED", "Your password was changed.", Map.of());

        return Map.of("message", "Password has been reset successfully. You can now sign in.");
    }

    @Transactional
    public Map<String, String> setPassword(SetPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TOKEN_REQUIRED", "Invitation token is required");
        }

        String hash = TokenUtils.hashToken(req.getToken().trim());
        EmailToken token = emailTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "INVITE_LINK_INVALID",
                        "This invitation link is expired or invalid."));

        if (token.getType() != TokenType.INVITE || token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVITE_LINK_INVALID",
                    "This invitation link is expired or invalid.");
        }

        User user = token.getUser();
        ValidationUtils.validatePassword(req.getPassword(), user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);

        return Map.of("message", "Password set successfully. Your account is now active. Please sign in.");
    }

    private void issueRefreshToken(User user, HttpServletResponse response) {
        String rawToken = TokenUtils.generateRandomToken();
        String hash = TokenUtils.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, hash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        // BR-8: httpOnly, SameSite=Strict cookie
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(REFRESH_TOKEN_DAYS * 24 * 3600);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
