package com.taskmanagesys.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.auth.dto.*;
import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.auth.service.AuthService;
import com.taskmanagesys.auth.service.RateLimiterService;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "CORS_ALLOWED_ORIGINS=https://allowed.example")
class AuthIntegrationTest {

    private static final String APP_ORIGIN = "http://localhost:5173";

    @Test
    void corsPreflightForAppOriginIsAcceptedBeforeAuthentication() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", APP_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", APP_ORIGIN));
    }

    @Test
    void loginFromConfiguredOriginsIsAcceptedAndUnrelatedOriginIsRejected() throws Exception {
        LoginRequest request = new LoginRequest("manager@gmail.com", "Manager123!");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", APP_ORIGIN));

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "https://allowed.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://allowed.example"));

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "https://unrelated.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private com.taskmanagesys.audit.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        refreshTokenRepository.deleteAll();
        emailTokenRepository.deleteAll();
        emailOutboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase("manager@gmail.com"))
                .forEach(userRepository::delete);
    }

    @Test
    void testSignUpFlowAndEmailVerification() throws Exception {
        String email = "test.applicant1@gmail.com";
        SignUpRequest req = new SignUpRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail(email);
        req.setPhone("01012345678");
        req.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Please check your email")));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertEquals(UserStatus.PENDING_EMAIL, user.getStatus());
        assertEquals("+201012345678", user.getPhone()); // E.164 normalized

        // Check email queued in outbox
        List<EmailOutbox> outbox = emailOutboxRepository.findAll();
        boolean foundVerifyEmail = outbox.stream()
                .anyMatch(e -> e.getToEmail().equals(email) && e.getTemplate().equals("VERIFY_EMAIL"));
        assertTrue(foundVerifyEmail, "VERIFY_EMAIL should be in outbox");

        // Obtain token from email_tokens
        List<EmailToken> tokens = emailTokenRepository.findAll();
        EmailToken tokenEntity = tokens.stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && t.getType() == TokenType.VERIFY_EMAIL)
                .findFirst().orElseThrow();

        // Extract token from queued payload
        EmailOutbox verifyOutbox = outbox.stream()
                .filter(e -> e.getToEmail().equals(email) && e.getTemplate().equals("VERIFY_EMAIL"))
                .findFirst().orElseThrow();
        String payloadJson = verifyOutbox.getPayload();
        String rawToken = objectMapper.readTree(payloadJson).get("token").asText();

        // Call verify-email endpoint
        mockMvc.perform(get("/api/auth/verify-email").param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("pending Manager approval")));

        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(UserStatus.PENDING_APPROVAL, verifiedUser.getStatus());
    }

    @Test
    void testLoginWithLockoutAfterFiveFailedAttempts() throws Exception {
        String email = "lockout.test@gmail.com";
        User user = new User();
        user.setFirstName("Lock");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("+201011112222");
        user.setPasswordHash(passwordEncoder.encode("ValidPass123!"));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest wrongLogin = new LoginRequest(email, "WrongPass123!");

        // 4 failed attempts
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        // 5th failed attempt -> locks account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized());

        User lockedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(5, lockedUser.getFailedLoginCount());
        assertNotNull(lockedUser.getLockedUntil());

        // 6th attempt (even with valid password) -> 423 LOCKED
        LoginRequest validLogin = new LoginRequest(email, "ValidPass123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void testSuccessfulLoginAndTokenRotation() throws Exception {
        String email = "jwt.session@gmail.com";
        User user = new User();
        user.setFirstName("Session");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPhone("+201022223333");
        user.setPasswordHash(passwordEncoder.encode("SessionPass123!"));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest req = new LoginRequest(email, "SessionPass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(cookie().exists(AuthService.REFRESH_COOKIE_NAME))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertNotNull(refreshCookie);
        assertTrue(refreshCookie.isHttpOnly());

        // Test refresh token rotation
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists(AuthService.REFRESH_COOKIE_NAME))
                .andReturn();

        Cookie newRefreshCookie = refreshResult.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertNotNull(newRefreshCookie);
        assertNotEquals(refreshCookie.getValue(), newRefreshCookie.getValue(), "Refresh token must rotate");

        // Old refresh token must now be invalid
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());

        // Test logout revokes token
        mockMvc.perform(post("/api/auth/logout").cookie(newRefreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(newRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testForgotPasswordAndResetPasswordFlow() throws Exception {
        String email = "reset.user@gmail.com";
        User user = new User();
        user.setFirstName("Reset");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("+201033334444");
        user.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("If the account exists")));

        // Find queued PASSWORD_RESET email
        List<EmailOutbox> outbox = emailOutboxRepository.findAll();
        EmailOutbox resetEmail = outbox.stream()
                .filter(e -> e.getToEmail().equals(email) && e.getTemplate().equals("PASSWORD_RESET"))
                .findFirst().orElseThrow();

        String rawToken = objectMapper.readTree(resetEmail.getPayload()).get("token").asText();

        // Attempt reset with same password -> 400
        ResetPasswordRequest samePassReq = new ResetPasswordRequest();
        samePassReq.setToken(rawToken);
        samePassReq.setNewPassword("OldPassword123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_SAME"));

        // Reset with new password
        ResetPasswordRequest validReset = new ResetPasswordRequest();
        validReset.setToken(rawToken);
        validReset.setNewPassword("BrandNewPass123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReset)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("reset successfully")));

        // Token must now be used
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_LINK_INVALID"));

        // Login with new password
        LoginRequest newLogin = new LoginRequest(email, "BrandNewPass123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk());
    }

    @Test
    void testSignUpRateLimitAfter10RequestsPerIP() throws Exception {
        SignUpRequest req = new SignUpRequest();
        req.setFirstName("Rate");
        req.setLastName("Limit");
        req.setPhone("01099998888");
        req.setPassword("Password123!");

        for (int i = 1; i <= 10; i++) {
            req.setEmail("ratelimit" + i + "@gmail.com");
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        // 11th request -> 429 TOO_MANY_REQUESTS
        req.setEmail("ratelimit11@gmail.com");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
