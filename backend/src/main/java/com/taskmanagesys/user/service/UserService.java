package com.taskmanagesys.user.service;

import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.auth.service.TokenUtils;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.common.validation.ValidationUtils;
import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.user.dto.ChangePasswordRequest;
import com.taskmanagesys.user.dto.ManagerEmailPrefsDto;
import com.taskmanagesys.user.dto.UpdateProfileRequest;
import com.taskmanagesys.user.dto.UserProfileResponse;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ManagerEmailPrefsRepository managerEmailPrefsRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;
    private final EmailOutboxService emailOutboxService;
    private final NotificationService notifications;

    public UserService(
            UserRepository userRepository,
            ManagerEmailPrefsRepository managerEmailPrefsRepository,
            EmailTokenRepository emailTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AvatarService avatarService,
            EmailOutboxService emailOutboxService,
            NotificationService notifications) {
        this.userRepository = userRepository;
        this.managerEmailPrefsRepository = managerEmailPrefsRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
        this.emailOutboxService = emailOutboxService;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        ManagerEmailPrefsDto prefsDto = null;
        if (user.getRole() == Role.MANAGER) {
            ManagerEmailPrefs prefs = managerEmailPrefsRepository.findById(userId).orElse(null);
            prefsDto = ManagerEmailPrefsDto.from(prefs);
        }
        return UserProfileResponse.from(user, prefsDto);
    }

    @Transactional
    public Map<String, Object> updateProfile(Long userId, UpdateProfileRequest req) {
        User user = getUser(userId);

        String firstName = ValidationUtils.validateAndNormalizeName(req.getFirstName(), "First name");
        String lastName = ValidationUtils.validateAndNormalizeName(req.getLastName(), "Last name");
        String phone = ValidationUtils.validateAndNormalizePhone(req.getPhone());
        String email = ValidationUtils.validateAndNormalizeEmail(req.getEmail());

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);

        boolean emailChanged = false;
        // BR-11: Changing email sends confirmation link to new address. Old email keeps working until link opened.
        if (!email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email address is already in use");
            }

            emailTokenRepository.invalidateActiveTokensForUser(user, TokenType.CHANGE_EMAIL);

            String rawToken = TokenUtils.generateRandomToken();
            String hash = TokenUtils.hashToken(rawToken);
            Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            EmailToken token = new EmailToken(user, TokenType.CHANGE_EMAIL, hash, expiresAt);
            token.setNewEmail(email);
            emailTokenRepository.save(token);

            notifications.accountEmailTo(email, user.getId(), "CONFIRM_NEW_EMAIL", "CONFIRM_NEW_EMAIL", "Confirm your new email address.", Map.of("token", rawToken));
            emailChanged = true;
        }

        userRepository.save(user);

        return Map.of(
                "message", emailChanged
                        ? "Profile updated. A confirmation link has been sent to your new email address."
                        : "Profile updated successfully.",
                "emailChanged", emailChanged
        );
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = getUser(userId);

        ValidationUtils.validatePassword(req.getNewPassword(), user.getEmail());

        // BR-10: Current password is required
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        // BR-10: New password must differ from current
        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PASSWORD_SAME",
                    "New password must differ from your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // BR-10: Other sessions are revoked
        refreshTokenRepository.revokeAllForUser(user);
    }

    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = getUser(userId);
        String savedFilename = avatarService.saveAvatar(file, user.getAvatarPath());
        user.setAvatarPath(savedFilename);
        userRepository.save(user);
        return savedFilename;
    }

    @Transactional
    public User removeAvatar(Long userId) {
        User user = getUser(userId);
        avatarService.deleteOldAvatar(user.getAvatarPath());
        user.setAvatarPath(null);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public byte[] getAvatar(Long userId) {
        User user = getUser(userId);
        return avatarService.loadAvatar(user.getAvatarPath());
    }

    @Transactional(readOnly = true)
    public String getAvatarContentType(Long userId) {
        User user = getUser(userId);
        return avatarService.getContentType(user.getAvatarPath());
    }

    @Transactional
    public void updateManagerEmailPrefs(Long managerUserId, ManagerEmailPrefsDto dto) {
        User user = getUser(managerUserId);
        if (user.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Email preferences are only available for Managers");
        }

        ManagerEmailPrefs prefs = managerEmailPrefsRepository.findById(managerUserId)
                .orElseGet(() -> new ManagerEmailPrefs(user));

        prefs.setEmailsEnabled(dto.isEmailsEnabled());
        prefs.setOnSignupRequest(dto.isOnSignupRequest());
        prefs.setOnTaskCompleted(dto.isOnTaskCompleted());
        prefs.setOnArchiveWarning(dto.isOnArchiveWarning());
        prefs.setOnSelfReported(dto.isOnSelfReported());

        managerEmailPrefsRepository.save(prefs);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }
}
