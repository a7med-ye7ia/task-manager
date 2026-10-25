package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.common.dto.PageResponse;
import com.taskmanagesys.user.dto.ApproveSignupRequest;
import com.taskmanagesys.user.dto.RejectSignupRequest;
import com.taskmanagesys.user.dto.UserDto;
import com.taskmanagesys.user.service.UserAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/pending")
@PreAuthorize("hasRole('MANAGER')")
public class AdminPendingController {

    private final UserAdminService userAdminService;

    public AdminPendingController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> listPending(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<UserDto> pending = userAdminService.listPendingSignups(page, size);
        return ResponseEntity.ok(PageResponse.from(pending));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countPending() {
        long count = userAdminService.countPendingSignups();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody(required = false) ApproveSignupRequest request) {
        ApproveSignupRequest req = request != null ? request : new ApproveSignupRequest();
        userAdminService.approveSignup(principal.getId(), id, req);
        return ResponseEntity.ok(Map.of("message", "Sign-up request approved successfully."));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody(required = false) RejectSignupRequest request) {
        RejectSignupRequest req = request != null ? request : new RejectSignupRequest();
        userAdminService.rejectSignup(principal.getId(), id, req);
        return ResponseEntity.ok(Map.of("message", "Sign-up request rejected successfully."));
    }
}
