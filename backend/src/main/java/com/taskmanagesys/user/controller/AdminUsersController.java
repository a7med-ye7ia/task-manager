package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.common.dto.PageResponse;
import com.taskmanagesys.user.dto.EditUserRequest;
import com.taskmanagesys.user.dto.InviteUserRequest;
import com.taskmanagesys.user.dto.UserDto;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.service.UserAdminService;
import com.taskmanagesys.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('MANAGER')")
public class AdminUsersController {

    private final UserAdminService userAdminService;
    private final UserService userService;

    public AdminUsersController(UserAdminService userAdminService, UserService userService) {
        this.userAdminService = userAdminService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> listUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "status", required = false) UserStatus status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        Page<UserDto> users = userAdminService.listUsers(page, size, role, status, search, includeDeleted);
        return ResponseEntity.ok(PageResponse.from(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable("id") Long id) {
        UserDto user = userAdminService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping({"", "/invite"})
    public ResponseEntity<UserDto> inviteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InviteUserRequest request) {
        UserDto user = userAdminService.inviteUser(principal.getId(), request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<Map<String, String>> resendInvite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        Map<String, String> response = userAdminService.resendInvite(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> editUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody EditUserRequest request) {
        UserDto user = userAdminService.editUser(principal.getId(), id, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        userAdminService.deactivateUser(principal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "User has been deactivated successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        userAdminService.softDeleteUser(principal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "User has been deleted successfully."));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, String>> restoreUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        userAdminService.restoreUser(principal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "User has been restored successfully."));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable("id") Long id) {
        byte[] bytes = userService.getAvatar(id);
        String contentType = userService.getAvatarContentType(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(bytes);
    }
}

