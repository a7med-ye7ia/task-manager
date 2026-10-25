package com.taskmanagesys.task.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.task.dto.CreateSubtaskRequest;
import com.taskmanagesys.task.dto.SubtaskResponse;
import com.taskmanagesys.task.dto.UpdateSubtaskRequest;
import com.taskmanagesys.task.service.SubtaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}/subtasks")
public class SubtaskController {

    private final SubtaskService subtaskService;

    public SubtaskController(SubtaskService subtaskService) {
        this.subtaskService = subtaskService;
    }

    @PostMapping
    public ResponseEntity<SubtaskResponse> createSubtask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateSubtaskRequest request) {
        SubtaskResponse response = subtaskService.createSubtask(taskId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{subtaskId}")
    public ResponseEntity<SubtaskResponse> updateSubtask(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateSubtaskRequest request) {
        return ResponseEntity.ok(subtaskService.updateSubtask(taskId, subtaskId, currentUser.getId(), request));
    }

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        subtaskService.deleteSubtask(taskId, subtaskId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{subtaskId}/assignees/{userId}")
    public ResponseEntity<SubtaskResponse> addAssignee(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "false") boolean confirmAddMember) {
        return ResponseEntity.ok(subtaskService.assignUserToSubtask(taskId, subtaskId, userId, currentUser.getId(), confirmAddMember));
    }

    @DeleteMapping("/{subtaskId}/assignees/{userId}")
    public ResponseEntity<SubtaskResponse> removeAssignee(
            @PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(subtaskService.removeUserFromSubtask(taskId, subtaskId, userId, currentUser.getId()));
    }
}
