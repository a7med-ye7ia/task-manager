package com.taskmanagesys.user.service;

import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.auth.model.EmailToken;
import com.taskmanagesys.auth.model.TokenType;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.auth.service.TokenUtils;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.common.validation.ValidationUtils;
import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.user.dto.*;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final ManagerEmailPrefsRepository managerEmailPrefsRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailOutboxService emailOutboxService;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notifications;

    @Value("${app.timezone:Africa/Cairo}")
    private String appTimezone;

    public UserAdminService(
            UserRepository userRepository,
            ManagerEmailPrefsRepository managerEmailPrefsRepository,
            EmailTokenRepository emailTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailOutboxService emailOutboxService,
            AuditLogService auditLogService,
            JdbcTemplate jdbcTemplate,
            NotificationService notifications) {
        this.userRepository = userRepository;
        this.managerEmailPrefsRepository = managerEmailPrefsRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailOutboxService = emailOutboxService;
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(int page, int size, Role role, UserStatus status, String search, boolean includeDeleted) {
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize, Sort.by(Sort.Direction.DESC, "id"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeDeleted) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchFirst = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate searchLast = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate searchEmail = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(searchFirst, searchLast, searchEmail));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long targetUserId) {
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    @Transactional
    public UserDto inviteUser(Long managerId, InviteUserRequest req) {
        String firstName = ValidationUtils.validateAndNormalizeName(req.getFirstName(), "First name");
        String lastName = ValidationUtils.validateAndNormalizeName(req.getLastName(), "Last name");
        String email = ValidationUtils.validateAndNormalizeEmail(req.getEmail());
        String phone = ValidationUtils.validateAndNormalizePhone(req.getPhone());
        Role role = req.getRole() != null ? req.getRole() : Role.EMPLOYEE;

        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email address already registered");
        }

        LocalDate hiredAt = req.getHiredAt() != null ? req.getHiredAt() : LocalDate.now(ZoneId.of(appTimezone));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(UserStatus.INVITED);
        user.setHiredAt(hiredAt);
        user.setFailedLoginCount(0);
        User savedUser = userRepository.save(user);

        if (role == Role.MANAGER) {
            ManagerEmailPrefs prefs = new ManagerEmailPrefs(savedUser);
            managerEmailPrefsRepository.save(prefs);
        }

        String rawToken = TokenUtils.generateRandomToken();
        String hash = TokenUtils.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(48, ChronoUnit.HOURS);

        EmailToken emailToken = new EmailToken(savedUser, TokenType.INVITE, hash, expiresAt);
        emailTokenRepository.save(emailToken);

        notifications.accountEmail(savedUser.getId(), "ACCOUNT_INVITE", "ACCOUNT_INVITE", "You have been invited to join the task manager.", Map.of("token", rawToken));

        auditLogService.record(managerId, savedUser.getId(), "USER_INVITED",
                Map.of("email", email, "role", role.name()));

        return UserDto.from(savedUser);
    }

    @Transactional
    public Map<String, String> resendInvite(Long managerId, Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.INVITED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "User is not in INVITED status");
        }

        emailTokenRepository.invalidateActiveTokensForUser(user, TokenType.INVITE);

        String rawToken = TokenUtils.generateRandomToken();
        String hash = TokenUtils.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(48, ChronoUnit.HOURS);

        EmailToken emailToken = new EmailToken(user, TokenType.INVITE, hash, expiresAt);
        emailTokenRepository.save(emailToken);

        notifications.accountEmail(user.getId(), "ACCOUNT_INVITE", "ACCOUNT_INVITE", "Your invitation link has been resent.", Map.of("token", rawToken));

        auditLogService.record(managerId, targetUserId, "USER_INVITE_RESENT", Map.of("email", user.getEmail()));

        return Map.of("message", "Invitation resent successfully.");
    }

    @Transactional
    public UserDto editUser(Long managerId, Long targetUserId, EditUserRequest req) {
        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String firstName = ValidationUtils.validateAndNormalizeName(req.getFirstName(), "First name");
        String lastName = ValidationUtils.validateAndNormalizeName(req.getLastName(), "Last name");
        String phone = ValidationUtils.validateAndNormalizePhone(req.getPhone());
        String email = ValidationUtils.validateAndNormalizeEmail(req.getEmail());
        Role newRole = req.getRole();

        // Safety rule: Manager cannot demote themselves
        if (managerId.equals(targetUserId) && user.getRole() == Role.MANAGER && newRole != Role.MANAGER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DEMOTE_SELF", "You cannot demote your own account.");
        }

        // BR-4: The last active Manager can never be demoted
        if (user.getRole() == Role.MANAGER && newRole != Role.MANAGER && user.getStatus() == UserStatus.ACTIVE) {
            if (userRepository.countActiveManagers() <= 1) {
                throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DEMOTE_LAST_MANAGER",
                        "The last active Manager can never be demoted.");
            }
        }

        // Email update handling
        if (!email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email address already registered");
            }
            emailTokenRepository.invalidateActiveTokensForUser(user, TokenType.CHANGE_EMAIL);

            String rawToken = TokenUtils.generateRandomToken();
            String hash = TokenUtils.hashToken(rawToken);
            Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            EmailToken token = new EmailToken(user, TokenType.CHANGE_EMAIL, hash, expiresAt);
            token.setNewEmail(email);
            emailTokenRepository.save(token);

            notifications.accountEmail(user.getId(), "CONFIRM_NEW_EMAIL", "CONFIRM_NEW_EMAIL", "Confirm your new email address.", Map.of("token", rawToken));
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setRole(newRole);

        if (newRole == Role.MANAGER && !managerEmailPrefsRepository.existsById(targetUserId)) {
            managerEmailPrefsRepository.save(new ManagerEmailPrefs(user));
        }

        User updatedUser = userRepository.save(user);

        auditLogService.record(managerId, targetUserId, "USER_EDITED",
                Map.of("role", newRole.name(), "email", email));

        return UserDto.from(updatedUser);
    }

    @Transactional
    public void deactivateUser(Long managerId, Long targetUserId) {
        // Safety rule: Manager cannot deactivate themselves
        if (managerId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DEACTIVATE_SELF", "You cannot deactivate your own account.");
        }

        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        // BR-4: The last active Manager can never be deactivated
        if (user.getRole() == Role.MANAGER && user.getStatus() == UserStatus.ACTIVE) {
            if (userRepository.countActiveManagers() <= 1) {
                throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DEACTIVATE_LAST_MANAGER",
                        "The last active Manager can never be deactivated.");
            }
        }

        user.setStatus(UserStatus.DISABLED);
        user.setDeactivatedAt(LocalDate.now(ZoneId.of(appTimezone)));
        userRepository.save(user);

        // BR-13: Sessions revoked
        refreshTokenRepository.revokeAllForUser(user);

        // BR-13: Removed from task_members and subtask_assignees
        cleanupTaskAssignments(targetUserId);

        auditLogService.record(managerId, targetUserId, "USER_DEACTIVATED", Map.of());
    }

    @Transactional
    public void softDeleteUser(Long managerId, Long targetUserId) {
        // Safety rule: Manager cannot delete themselves
        if (managerId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SELF", "You cannot delete your own account.");
        }

        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        // BR-4: The last active Manager can never be deleted
        if (user.getRole() == Role.MANAGER && user.getStatus() == UserStatus.ACTIVE) {
            if (userRepository.countActiveManagers() <= 1) {
                throw new AppException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_LAST_MANAGER",
                        "The last active Manager can never be deleted.");
            }
        }

        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        // BR-13: Sessions revoked
        refreshTokenRepository.revokeAllForUser(user);

        // BR-13: Removed from task_members and subtask_assignees
        cleanupTaskAssignments(targetUserId);

        auditLogService.record(managerId, targetUserId, "USER_DELETED", Map.of());
    }

    @Transactional
    public void restoreUser(Long managerId, Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        // BR-13: Restore clears deleted_at and deactivated_at, status ACTIVE
        user.setDeletedAt(null);
        user.setDeactivatedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        auditLogService.record(managerId, targetUserId, "USER_RESTORED", Map.of());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> listPendingSignups(int page, int size) {
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        return userRepository.findByStatusAndDeletedAtIsNull(UserStatus.PENDING_APPROVAL, pageable).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public long countPendingSignups() {
        return userRepository.countPendingApprovals();
    }

    @Transactional
    public void approveSignup(Long managerId, Long targetUserId, ApproveSignupRequest req) {
        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "User is not pending approval");
        }

        Role role = req.getRole() != null ? req.getRole() : Role.EMPLOYEE;
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setHiredAt(LocalDate.now(ZoneId.of(appTimezone)));
        userRepository.save(user);

        if (role == Role.MANAGER && !managerEmailPrefsRepository.existsById(targetUserId)) {
            managerEmailPrefsRepository.save(new ManagerEmailPrefs(user));
        }

        notifications.accountEmail(user.getId(), "SIGNUP_APPROVED", "SIGNUP_APPROVED", "Your sign-up request was approved.", Map.of());

        auditLogService.record(managerId, targetUserId, "SIGNUP_APPROVED", Map.of("role", role.name()));
    }

    @Transactional
    public void rejectSignup(Long managerId, Long targetUserId, RejectSignupRequest req) {
        User user = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "User is not pending approval");
        }

        String reason = req.getReason() != null ? req.getReason().trim() : "";
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        notifications.accountEmail(user.getId(), "SIGNUP_REJECTED", "SIGNUP_REJECTED", "Your sign-up request was rejected.", Map.of("reason", reason));

        auditLogService.record(managerId, targetUserId, "SIGNUP_REJECTED",
                reason.isEmpty() ? Map.of() : Map.of("reason", reason));
    }

    private void cleanupTaskAssignments(Long userId) {
        // BR-13: A deactivated or deleted user is removed from task_members and subtask_assignees
        try {
            jdbcTemplate.update("DELETE FROM task_members WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM subtask_assignees WHERE user_id = ?", userId);
        } catch (Exception ignored) {}
    }
}
