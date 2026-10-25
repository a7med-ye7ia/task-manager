package com.taskmanagesys.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.auth.service.JwtService;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.user.dto.ApproveSignupRequest;
import com.taskmanagesys.user.dto.EditUserRequest;
import com.taskmanagesys.user.dto.InviteUserRequest;
import com.taskmanagesys.user.dto.RejectSignupRequest;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private com.taskmanagesys.auth.repository.EmailTokenRepository emailTokenRepository;

    @Autowired
    private com.taskmanagesys.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.taskmanagesys.audit.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User managerUser;
    private String managerToken;
    private User employeeUser;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        emailTokenRepository.deleteAll();
        emailOutboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase("manager@gmail.com"))
                .forEach(userRepository::delete);

        managerUser = userRepository.findByEmail("manager@gmail.com").orElseGet(() -> {
            User m = new User();
            m.setFirstName("System");
            m.setLastName("Manager");
            m.setEmail("manager@gmail.com");
            m.setPhone("+201000000000");
            m.setPasswordHash(passwordEncoder.encode("Manager123!"));
            m.setRole(Role.MANAGER);
            m.setStatus(UserStatus.ACTIVE);
            m.setHiredAt(LocalDate.now());
            return userRepository.save(m);
        });
        managerToken = jwtService.generateAccessToken(managerUser);

        employeeUser = userRepository.findByEmail("employee.test@gmail.com").orElseGet(() -> {
            User e = new User();
            e.setFirstName("Emp");
            e.setLastName("Loyee");
            e.setEmail("employee.test@gmail.com");
            e.setPhone("+201011119999");
            e.setPasswordHash(passwordEncoder.encode("Password123!"));
            e.setRole(Role.EMPLOYEE);
            e.setStatus(UserStatus.ACTIVE);
            e.setHiredAt(LocalDate.now());
            return userRepository.save(e);
        });
        employeeToken = jwtService.generateAccessToken(employeeUser);
    }

    @Test
    void testEmployeeCannotAccessAdminEndpointsReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // 1. GET /api/admin/users
    @Test
    void testEndpoint_listUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("role", "EMPLOYEE")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].email", is(employeeUser.getEmail())));
    }

    // 2. GET /api/admin/users/{id}
    @Test
    void testEndpoint_getUser() throws Exception {
        mockMvc.perform(get("/api/admin/users/" + employeeUser.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employeeUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is(employeeUser.getEmail())))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")));
    }

    // 3. POST /api/admin/users (and /invite)
    @Test
    void testEndpoint_inviteUser() throws Exception {
        InviteUserRequest req = new InviteUserRequest();
        req.setFirstName("Invited");
        req.setLastName("User");
        req.setEmail("invited.person@gmail.com");
        req.setPhone("01011112222");
        req.setRole(Role.EMPLOYEE);
        req.setHiredAt(LocalDate.now());

        // Test POST /api/admin/users
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("invited.person@gmail.com")))
                .andExpect(jsonPath("$.status", is("INVITED")));

        // Test alias POST /api/admin/users/invite
        InviteUserRequest req2 = new InviteUserRequest();
        req2.setFirstName("InvitedTwo");
        req2.setLastName("User");
        req2.setEmail("invited.person2@gmail.com");
        req2.setPhone("01022223333");
        req2.setRole(Role.EMPLOYEE);

        mockMvc.perform(post("/api/admin/users/invite")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("invited.person2@gmail.com")));
    }

    // 4. POST /api/admin/users/{id}/resend-invite
    @Test
    void testEndpoint_resendInvite() throws Exception {
        User invited = new User();
        invited.setFirstName("Resend");
        invited.setLastName("Target");
        invited.setEmail("resend.invite@gmail.com");
        invited.setPhone("+201099998888");
        invited.setRole(Role.EMPLOYEE);
        invited.setStatus(UserStatus.INVITED);
        invited = userRepository.save(invited);

        mockMvc.perform(post("/api/admin/users/" + invited.getId() + "/resend-invite")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Invitation resent successfully")));
    }

    // 5. PUT /api/admin/users/{id}
    @Test
    void testEndpoint_editUser() throws Exception {
        EditUserRequest req = new EditUserRequest();
        req.setFirstName("EditedFirst");
        req.setLastName("EditedLast");
        req.setEmail(employeeUser.getEmail());
        req.setPhone("+201011119999");
        req.setRole(Role.EMPLOYEE);

        mockMvc.perform(put("/api/admin/users/" + employeeUser.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("EditedFirst")))
                .andExpect(jsonPath("$.lastName", is("EditedLast")));
    }

    // 6. POST /api/admin/users/{id}/deactivate
    @Test
    void testEndpoint_deactivateUser() throws Exception {
        User activeUser = new User();
        activeUser.setFirstName("ToDeact");
        activeUser.setLastName("User");
        activeUser.setEmail("todeact@gmail.com");
        activeUser.setPhone("+201077776666");
        activeUser.setRole(Role.EMPLOYEE);
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser = userRepository.save(activeUser);

        mockMvc.perform(post("/api/admin/users/" + activeUser.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deactivated successfully")));

        User reloaded = userRepository.findById(activeUser.getId()).orElseThrow();
        assertEquals(UserStatus.DISABLED, reloaded.getStatus());
        assertNotNull(reloaded.getDeactivatedAt());
    }

    // 7. DELETE /api/admin/users/{id}
    @Test
    void testEndpoint_deleteUser() throws Exception {
        User userToDelete = new User();
        userToDelete.setFirstName("ToDelete");
        userToDelete.setLastName("User");
        userToDelete.setEmail("todelete@gmail.com");
        userToDelete.setPhone("+201055556666");
        userToDelete.setRole(Role.EMPLOYEE);
        userToDelete.setStatus(UserStatus.ACTIVE);
        userToDelete = userRepository.save(userToDelete);

        mockMvc.perform(delete("/api/admin/users/" + userToDelete.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        User reloaded = userRepository.findById(userToDelete.getId()).orElseThrow();
        assertNotNull(reloaded.getDeletedAt());
    }

    // 8. POST /api/admin/users/{id}/restore
    @Test
    void testEndpoint_restoreUser() throws Exception {
        User disabledUser = new User();
        disabledUser.setFirstName("ToRestore");
        disabledUser.setLastName("User");
        disabledUser.setEmail("torestore@gmail.com");
        disabledUser.setPhone("+201044443333");
        disabledUser.setRole(Role.EMPLOYEE);
        disabledUser.setStatus(UserStatus.DISABLED);
        disabledUser.setDeactivatedAt(LocalDate.now());
        disabledUser = userRepository.save(disabledUser);

        mockMvc.perform(post("/api/admin/users/" + disabledUser.getId() + "/restore")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("restored successfully")));

        User reloaded = userRepository.findById(disabledUser.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, reloaded.getStatus());
        assertNull(reloaded.getDeactivatedAt());
    }

    // 9. GET /api/admin/pending
    @Test
    void testEndpoint_listPending() throws Exception {
        User pendingApplicant = new User();
        pendingApplicant.setFirstName("Pending");
        pendingApplicant.setLastName("Applicant");
        pendingApplicant.setEmail("pending.list@gmail.com");
        pendingApplicant.setPhone("+201022221111");
        pendingApplicant.setRole(Role.EMPLOYEE);
        pendingApplicant.setStatus(UserStatus.PENDING_APPROVAL);
        userRepository.save(pendingApplicant);

        mockMvc.perform(get("/api/admin/pending")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    // 10. GET /api/admin/pending/count
    @Test
    void testEndpoint_countPending() throws Exception {
        User pendingApplicant = new User();
        pendingApplicant.setFirstName("Count");
        pendingApplicant.setLastName("Applicant");
        pendingApplicant.setEmail("count.pending@gmail.com");
        pendingApplicant.setPhone("+201033332222");
        pendingApplicant.setRole(Role.EMPLOYEE);
        pendingApplicant.setStatus(UserStatus.PENDING_APPROVAL);
        userRepository.save(pendingApplicant);

        mockMvc.perform(get("/api/admin/pending/count")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
    }

    // 11. POST /api/admin/pending/{id}/approve
    @Test
    void testEndpoint_approvePending() throws Exception {
        User applicant = new User();
        applicant.setFirstName("Approve");
        applicant.setLastName("Target");
        applicant.setEmail("approve.target@gmail.com");
        applicant.setPhone("+201088881111");
        applicant.setRole(Role.EMPLOYEE);
        applicant.setStatus(UserStatus.PENDING_APPROVAL);
        applicant = userRepository.save(applicant);

        ApproveSignupRequest req = new ApproveSignupRequest(Role.EMPLOYEE);
        mockMvc.perform(post("/api/admin/pending/" + applicant.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("approved successfully")));

        User reloaded = userRepository.findById(applicant.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, reloaded.getStatus());
    }

    // 12. POST /api/admin/pending/{id}/reject
    @Test
    void testEndpoint_rejectPending() throws Exception {
        User applicant = new User();
        applicant.setFirstName("Reject");
        applicant.setLastName("Target");
        applicant.setEmail("reject.target@gmail.com");
        applicant.setPhone("+201099992222");
        applicant.setRole(Role.EMPLOYEE);
        applicant.setStatus(UserStatus.PENDING_APPROVAL);
        applicant = userRepository.save(applicant);

        RejectSignupRequest req = new RejectSignupRequest("Did not meet requirements");
        mockMvc.perform(post("/api/admin/pending/" + applicant.getId() + "/reject")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("rejected successfully")));

        User reloaded = userRepository.findById(applicant.getId()).orElseThrow();
        assertEquals(UserStatus.REJECTED, reloaded.getStatus());
    }

    @Test
    void testBR4LastActiveManagerProtection() throws Exception {
        // Ensure there is only 1 active manager
        List<User> managers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER && u.getStatus() == UserStatus.ACTIVE && u.getDeletedAt() == null)
                .toList();

        for (int i = 1; i < managers.size(); i++) {
            userRepository.delete(managers.get(i));
        }

        assertEquals(1, userRepository.countActiveManagers(), "Should be exactly 1 active manager for BR-4 test");

        User singleManager = userRepository.findByEmail("manager@gmail.com").orElseThrow();

        // Self-management guards are deliberately checked before the last-manager
        // guard: a Manager may never change their own role, deactivate, or delete
        // their own account.
        EditUserRequest demoteReq = new EditUserRequest();
        demoteReq.setFirstName(singleManager.getFirstName());
        demoteReq.setLastName(singleManager.getLastName());
        demoteReq.setEmail(singleManager.getEmail());
        demoteReq.setPhone(singleManager.getPhone());
        demoteReq.setRole(Role.EMPLOYEE);

        mockMvc.perform(put("/api/admin/users/" + singleManager.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DEMOTE_SELF"));

        // 2. Attempt to deactivate -> 400 CANNOT_DEACTIVATE_LAST_MANAGER
        mockMvc.perform(post("/api/admin/users/" + singleManager.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));

        // 3. Attempt to delete -> 400 CANNOT_DELETE_LAST_MANAGER
        mockMvc.perform(delete("/api/admin/users/" + singleManager.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DELETE_SELF"));
    }
}
