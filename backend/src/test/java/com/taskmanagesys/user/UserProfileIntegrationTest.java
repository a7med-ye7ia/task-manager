package com.taskmanagesys.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.auth.dto.LoginRequest;
import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.RefreshToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.auth.service.JwtService;
import com.taskmanagesys.auth.service.TokenUtils;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.user.dto.ChangePasswordRequest;
import com.taskmanagesys.user.dto.ManagerEmailPrefsDto;
import com.taskmanagesys.user.dto.UpdateProfileRequest;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerEmailPrefsRepository managerEmailPrefsRepository;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.taskmanagesys.audit.repository.AuditLogRepository auditLogRepository;

    private User testEmployee;
    private User testManager;
    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        emailTokenRepository.deleteAll();
        emailOutboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase("manager@gmail.com"))
                .forEach(userRepository::delete);

        // Find or create test manager
        testManager = userRepository.findByEmail("prof_mgr@gmail.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("Manager");
            u.setLastName("User");
            u.setEmail("prof_mgr@gmail.com");
            u.setPhone("+201011112222");
            u.setPasswordHash(passwordEncoder.encode("ManagerPassword123"));
            u.setRole(Role.MANAGER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        // Find or create test employee
        testEmployee = userRepository.findByEmail("prof_emp@gmail.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("Employee");
            u.setLastName("User");
            u.setEmail("prof_emp@gmail.com");
            u.setPhone("+201033334444");
            u.setPasswordHash(passwordEncoder.encode("EmployeePassword123"));
            u.setRole(Role.EMPLOYEE);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        managerToken = jwtService.generateAccessToken(testManager);
        employeeToken = jwtService.generateAccessToken(testEmployee);
    }

    @Test
    void getProfile_authenticated_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testEmployee.getId().intValue())))
                .andExpect(jsonPath("$.email", is(testEmployee.getEmail())))
                .andExpect(jsonPath("$.firstName", is(testEmployee.getFirstName())))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void getProfile_unauthenticated_returns403Or401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_namesAndPhone_success() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("UpdatedFirst");
        req.setLastName("UpdatedLast");
        req.setEmail(testEmployee.getEmail());
        req.setPhone("+201012345678");

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailChanged", is(false)));

        User reloaded = userRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals("UpdatedFirst", reloaded.getFirstName());
        assertEquals("UpdatedLast", reloaded.getLastName());
        assertEquals("+201012345678", reloaded.getPhone());
    }

    @Test
    void updateProfile_emailChange_generatesTokenAndQueuesEmail() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Employee");
        req.setLastName("User");
        req.setEmail("new_employee_email@gmail.com");
        req.setPhone("+201012345678");

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailChanged", is(true)));

        // Old email is still the active email (BR-11)
        User reloaded = userRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals("prof_emp@gmail.com", reloaded.getEmail());

        // Token generated in email_tokens
        List<EmailToken> tokens = emailTokenRepository.findAll();
        assertEquals(1, tokens.size());
        EmailToken emailToken = tokens.get(0);
        assertEquals(TokenType.CHANGE_EMAIL, emailToken.getType());
        assertEquals("new_employee_email@gmail.com", emailToken.getNewEmail());

        // Outbox contains CONFIRM_NEW_EMAIL
        List<EmailOutbox> outbox = emailOutboxRepository.findAll();
        assertEquals(1, outbox.size());
        assertEquals("CONFIRM_NEW_EMAIL", outbox.get(0).getTemplate());
        assertEquals("new_employee_email@gmail.com", outbox.get(0).getToEmail());

        String rawToken = objectMapper.readTree(outbox.get(0).getPayload()).get("token").asText();
        mockMvc.perform(get("/api/auth/verify-email").param("token", rawToken))
                .andExpect(status().isOk());
        assertEquals("new_employee_email@gmail.com", userRepository.findById(testEmployee.getId()).orElseThrow().getEmail());
    }

    @Test
    void changePassword_validFlow_andRevokesOtherTokens() throws Exception {
        // Create an active refresh token for test employee
        RefreshToken refreshToken = new RefreshToken(
                testEmployee,
                TokenUtils.hashToken("dummy-refresh-token"),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);
        assertEquals(1, refreshTokenRepository.count());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("EmployeePassword123");
        req.setNewPassword("BrandNewPassword123");

        mockMvc.perform(post("/api/profile/change-password")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // All refresh tokens revoked (BR-10)
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
        assertTrue(remainingTokens.stream().allMatch(t -> t.getRevokedAt() != null));

        // Can login with new password
        LoginRequest loginReq = new LoginRequest("prof_emp@gmail.com", "BrandNewPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void changePassword_samePassword_failsWithBadRequest() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("EmployeePassword123");
        req.setNewPassword("EmployeePassword123");

        mockMvc.perform(post("/api/profile/change-password")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PASSWORD_SAME")));
    }

    @Test
    void changePassword_wrongCurrentPassword_failsWithBadRequest() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongCurrentPassword123");
        req.setNewPassword("BrandNewPassword123");

        mockMvc.perform(post("/api/profile/change-password")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_CURRENT_PASSWORD")));
    }

    @Test
    void avatar_uploadAndRetrieve_success() throws Exception {
        // Generate valid PNG image
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", validPngBytes
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPath", notNullValue()));

        // Retrieve avatar
        mockMvc.perform(get("/api/profile/avatar")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(validPngBytes));
    }

    @Test
    void avatar_uploadInvalidImage_failsWithBadRequest() throws Exception {
        byte[] fakeBytes = "This is not a real image at all".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", fakeBytes
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void avatar_remove_clearsPathAndDeletesAccess() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(img, "png", bytes);
        mockMvc.perform(multipart("/api/profile/avatar").file(new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray())).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/me/avatar").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarPath").value(nullValue()));
        assertNull(userRepository.findById(testEmployee.getId()).orElseThrow().getAvatarPath());
    }

    @Test
    void avatar_fileJustUnder2Mb_isAccepted() throws Exception {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] baseJpg = baos.toByteArray();

        // Pad to 1.7 MB (under 2 MB limit)
        byte[] jpg17Mb = new byte[1700 * 1024];
        System.arraycopy(baseJpg, 0, jpg17Mb, 0, baseJpg.length);

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar-1.7mb.jpg", "image/jpeg", jpg17Mb
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPath", notNullValue()));
    }


    @Test
    void avatar_uploadAndDownloadThroughProfileAndUserEndpoints() throws Exception {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] jpgBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file", "my-avatar.jpg", "image/jpeg", jpgBytes
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPath", notNullValue()));

        mockMvc.perform(get("/api/profile/avatar")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpgBytes));

        mockMvc.perform(get("/api/profile/avatar?token=" + employeeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpgBytes));

        mockMvc.perform(get("/api/users/" + testEmployee.getId() + "/avatar?token=" + employeeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpgBytes));
    }
    @Test
    void avatar_fileOver2Mb_isRejectedWithPayloadTooLarge() throws Exception {
        byte[] over2Mb = new byte[2 * 1024 * 1024 + 2048]; // 2 MB + 2 KB
        over2Mb[0] = (byte) 0xFF;
        over2Mb[1] = (byte) 0xD8;
        over2Mb[2] = (byte) 0xFF;

        MockMultipartFile file = new MockMultipartFile(
                "file", "huge-avatar.jpg", "image/jpeg", over2Mb
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code", is("PAYLOAD_TOO_LARGE")))
                .andExpect(jsonPath("$.message", is("The image must be 2 MB or smaller")));
    }

    @Test
    void emailPreferences_managerCanUpdate_employeeForbidden() throws Exception {
        ManagerEmailPrefsDto prefs = new ManagerEmailPrefsDto();
        prefs.setEmailsEnabled(true);
        prefs.setOnSignupRequest(false);
        prefs.setOnTaskCompleted(true);
        prefs.setOnArchiveWarning(true);

        // Manager updates successfully
        mockMvc.perform(put("/api/profile/email-preferences")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isOk());

        ManagerEmailPrefs savedPrefs = managerEmailPrefsRepository.findById(testManager.getId()).orElseThrow();
        assertTrue(savedPrefs.isEmailsEnabled());
        assertFalse(savedPrefs.isOnSignupRequest());
        assertTrue(savedPrefs.isOnTaskCompleted());

        // Employee receives 403 Forbidden
        mockMvc.perform(put("/api/profile/email-preferences")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isForbidden());
    }
}
