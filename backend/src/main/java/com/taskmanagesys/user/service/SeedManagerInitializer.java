package com.taskmanagesys.user.service;

import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Component
public class SeedManagerInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedManagerInitializer.class);

    private final UserRepository userRepository;
    private final ManagerEmailPrefsRepository managerEmailPrefsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-manager.first-name:System}")
    private String seedFirstName;

    @Value("${app.seed-manager.last-name:Manager}")
    private String seedLastName;

    @Value("${app.seed-manager.email:manager@gmail.com}")
    private String seedEmail;

    @Value("${app.seed-manager.password:Manager123!}")
    private String seedPassword;

    @Value("${app.seed-manager.phone:+201000000000}")
    private String seedPhone;

    @Value("${app.timezone:Africa/Cairo}")
    private String appTimezone;

    public SeedManagerInitializer(
            UserRepository userRepository,
            ManagerEmailPrefsRepository managerEmailPrefsRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.managerEmailPrefsRepository = managerEmailPrefsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<User> existingSeedManager = userRepository.findByEmail(seedEmail);
        if (existingSeedManager.isPresent()) {
            User manager = existingSeedManager.get();
            if (!managerEmailPrefsRepository.existsById(manager.getId())) {
                ManagerEmailPrefs prefs = new ManagerEmailPrefs(manager);
                managerEmailPrefsRepository.save(prefs);
                log.info("Initialized missing manager email preferences for seed manager: {}", seedEmail);
            }
        } else if (userRepository.count() == 0) {
            log.info("Users table is empty. Initializing seed Manager account: {}", seedEmail);

            User manager = new User();
            manager.setFirstName(seedFirstName);
            manager.setLastName(seedLastName);
            manager.setEmail(seedEmail);
            manager.setPhone(seedPhone);
            manager.setPasswordHash(passwordEncoder.encode(seedPassword));
            manager.setRole(Role.MANAGER);
            manager.setStatus(UserStatus.ACTIVE);
            manager.setHiredAt(LocalDate.now(ZoneId.of(appTimezone)));
            manager.setFailedLoginCount(0);

            User savedManager = userRepository.save(manager);

            ManagerEmailPrefs prefs = new ManagerEmailPrefs(savedManager);
            managerEmailPrefsRepository.save(prefs);

            log.info("Seed Manager initialized successfully with ID: {}", savedManager.getId());
        } else {
            log.info("Users exist in database. Seed manager initialization skipped.");
        }
    }
}
