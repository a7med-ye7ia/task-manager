package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.user.dto.ChangePasswordRequest;
import com.taskmanagesys.user.dto.ManagerEmailPrefsDto;
import com.taskmanagesys.user.dto.UpdateProfileRequest;
import com.taskmanagesys.user.dto.UserProfileResponse;
import com.taskmanagesys.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse response = userService.getProfile(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        Map<String, Object> result = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. All other sessions have been signed out."));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        String filename = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(Map.of("message", "Avatar uploaded successfully", "avatarPath", filename));
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] bytes = userService.getAvatar(principal.getId());
        String contentType = userService.getAvatarContentType(principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(bytes);
    }

    @PutMapping("/email-preferences")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> updateEmailPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ManagerEmailPrefsDto dto) {
        userService.updateManagerEmailPrefs(principal.getId(), dto);
        return ResponseEntity.ok(Map.of("message", "Email preferences updated successfully."));
    }
}

