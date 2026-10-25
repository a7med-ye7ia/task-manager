package com.taskmanagesys.notification;

import com.taskmanagesys.auth.service.JwtService;
import com.taskmanagesys.notification.model.Notification;
import com.taskmanagesys.notification.repository.NotificationRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    private User user;
    private User otherUser;
    private String token;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        user = users.findByEmail("notify.integration@example.com").orElseGet(() -> { User created = new User(); created.setFirstName("Notify"); created.setLastName("User"); created.setEmail("notify.integration@example.com"); created.setPhone("+201000000099"); created.setPasswordHash(encoder.encode("Password123#")); created.setRole(Role.EMPLOYEE); created.setStatus(UserStatus.ACTIVE); created.setHiredAt(LocalDate.now()); return users.save(created); });
        token = jwt.generateAccessToken(user);
        notifications.save(new Notification(user.getId(), "TEST", null, "Unread one"));
        notifications.save(new Notification(user.getId(), "TEST", null, "Unread two"));
        otherUser = users.findByEmail("notify.other@example.com").orElseGet(() -> { User created = new User(); created.setFirstName("Other"); created.setLastName("Notify"); created.setEmail("notify.other@example.com"); created.setPhone("+201000000098"); created.setPasswordHash(encoder.encode("Password123#")); created.setRole(Role.EMPLOYEE); created.setStatus(UserStatus.ACTIVE); created.setHiredAt(LocalDate.now()); return users.save(created); });
        notifications.save(new Notification(otherUser.getId(), "TEST", null, "Other unread"));
    }

    @Test
    void readAllMarksOnlyCurrentUsersNotificationsRead() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("0"));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].read").value(true)).andExpect(jsonPath("$[1].read").value(true));
        org.junit.jupiter.api.Assertions.assertEquals(1, notifications.findByUserIdAndReadFalse(otherUser.getId()).size());
    }

    @Test
    void clearAllRemovesOnlyCurrentUsersNotifications() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertEquals(0, notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 20)).size());
        org.junit.jupiter.api.Assertions.assertEquals(1, notifications.findByUserIdAndReadFalse(otherUser.getId()).size());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(content().string("0"));
    }

    @Test
    void clearAllRequiresAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
