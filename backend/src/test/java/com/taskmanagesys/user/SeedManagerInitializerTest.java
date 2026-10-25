package com.taskmanagesys.user;

import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import com.taskmanagesys.user.service.SeedManagerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedManagerInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerEmailPrefsRepository managerEmailPrefsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SeedManagerInitializer seedManagerInitializer;

    @Test
    void testSeedManagerCreatedOnStartup() {
        // The shared test database can have rows left by other integration tests.
        // Exercise the initializer against an empty users table instead of relying
        // on test-class execution order.
        managerEmailPrefsRepository.deleteAll();
        userRepository.deleteAll();
        seedManagerInitializer.run(null);

        Optional<User> managerOpt = userRepository.findByEmail("manager@gmail.com");
        assertTrue(managerOpt.isPresent(), "Seed manager should be created on startup");

        User manager = managerOpt.get();
        assertEquals("System", manager.getFirstName());
        assertEquals("Manager", manager.getLastName());
        assertEquals(Role.MANAGER, manager.getRole());
        assertEquals(UserStatus.ACTIVE, manager.getStatus());
        assertTrue(passwordEncoder.matches("Manager123!", manager.getPasswordHash()));
        assertNotNull(manager.getHiredAt());

        // Check manager email preferences (BR-36 & Assumption 7)
        Optional<ManagerEmailPrefs> prefsOpt = managerEmailPrefsRepository.findById(manager.getId());
        assertTrue(prefsOpt.isPresent(), "Manager email preferences should be created");

        ManagerEmailPrefs prefs = prefsOpt.get();
        assertTrue(prefs.isEmailsEnabled(), "Master switch should be enabled by default");
        assertTrue(prefs.isOnSignupRequest(), "on_signup_request should be enabled by default");
        assertFalse(prefs.isOnTaskCompleted(), "on_task_completed should be disabled by default");
        assertFalse(prefs.isOnArchiveWarning(), "on_archive_warning should be disabled by default");
    }
}
