package com.taskmanagesys.task.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.common.dto.PageResponse;
import com.taskmanagesys.task.dto.*;
import com.taskmanagesys.task.service.CheckpointService;
import com.taskmanagesys.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.taskmanagesys.task.model.TaskPriority;
import com.taskmanagesys.task.model.TaskStatus;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CheckpointService checkpointService;
    private final com.taskmanagesys.task.service.CommentService commentService;
    private final com.taskmanagesys.task.service.TaskActivityService taskActivityService;
    private final com.taskmanagesys.task.service.TaskAccessService taskAccessService;
    private final com.taskmanagesys.task.service.TaskReadStateService taskReadStateService;
    private final com.taskmanagesys.task.service.MentionService mentionService;

    public TaskController(TaskService taskService, CheckpointService checkpointService, com.taskmanagesys.task.service.CommentService commentService,
                          com.taskmanagesys.task.service.TaskActivityService taskActivityService, com.taskmanagesys.task.service.TaskAccessService taskAccessService, com.taskmanagesys.task.service.TaskReadStateService taskReadStateService, com.taskmanagesys.task.service.MentionService mentionService) {
        this.taskService = taskService;
        this.checkpointService = checkpointService;
        this.commentService = commentService;
        this.taskActivityService = taskActivityService;
        this.taskAccessService = taskAccessService;
        this.taskReadStateService = taskReadStateService;
        this.mentionService = mentionService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long memberUserId) {
        if (memberUserId != null) {
            return ResponseEntity.ok(taskService.getTasksForUser(memberUserId));
        }
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/pool")
    public ResponseEntity<PageResponse<TaskResponse>> getPool(@AuthenticationPrincipal UserPrincipal currentUser, @RequestParam(required=false) String search, @RequestParam(required=false) TaskStatus status, @RequestParam(required=false) TaskPriority priority, @RequestParam(required=false) java.time.LocalDate deadlineFrom, @RequestParam(required=false) java.time.LocalDate deadlineTo, @RequestParam(defaultValue="false") boolean myTasks, @RequestParam(defaultValue="false") boolean iLead, @RequestParam(defaultValue="false") boolean archived, @RequestParam(defaultValue="deadline") String sort, @RequestParam(defaultValue="0") int page) {
        return ResponseEntity.ok(PageResponse.from(taskService.getTaskPool(currentUser.getId(), search, status, priority, deadlineFrom, deadlineTo, myTasks, iLead, archived, sort, PageRequest.of(Math.max(0, page), 20))));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TaskResponse> getTaskDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.getTaskDetails(id));
    }

    @GetMapping("/{id:\\d+}/member-candidates")
    public ResponseEntity<PageResponse<MemberCandidateResponse>> memberCandidates(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser,
                                                                            @RequestParam(defaultValue = "") String query,
                                                                            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(PageResponse.from(taskService.getMemberCandidates(id, currentUser.getId(), query, PageRequest.of(Math.max(0, page), 20))));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, currentUser.getId(), request));
    }

    @PatchMapping("/{id:\\d+}/status")
    public ResponseEntity<TaskResponse> changeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangeTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, currentUser.getId(), request));
    }

    @PostMapping("/{id:\\d+}/send-to-review")
    public ResponseEntity<TaskResponse> sendToReview(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskService.sendToReview(id, currentUser.getId()));
    }

    @PostMapping("/{id:\\d+}/send-back")
    public ResponseEntity<TaskResponse> sendBack(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser,
                                                  @Valid @RequestBody SendBackTaskRequest request) {
        return ResponseEntity.ok(taskService.sendBack(id, currentUser.getId(), request));
    }

    @PostMapping("/{id:\\d+}/restore")
    public ResponseEntity<TaskResponse> restore(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser, @RequestParam TaskStatus targetStatus) {
        return ResponseEntity.ok(taskService.restoreTask(id, currentUser.getId(), targetStatus));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        taskService.deleteTask(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/members/{userId}") public ResponseEntity<TaskResponse> addMember(@PathVariable Long taskId, @PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) { return ResponseEntity.ok(taskService.addMember(taskId, currentUser.getId(), userId)); }
    @DeleteMapping("/{taskId}/members/{userId}") public ResponseEntity<TaskResponse> removeMember(@PathVariable Long taskId, @PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) { return ResponseEntity.ok(taskService.removeMember(taskId, currentUser.getId(), userId)); }
    @PutMapping("/{taskId}/lead/{userId}") public ResponseEntity<TaskResponse> changeLead(@PathVariable Long taskId, @PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) { return ResponseEntity.ok(taskService.changeLead(taskId, currentUser.getId(), userId)); }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> comments(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        mentionService.markTaskSeen(taskId, currentUser.getId());
        return ResponseEntity.ok(commentService.list(taskId, currentUser.getId()));
    }

    @GetMapping("/{taskId}/mentions/unread")
    public ResponseEntity<MentionCountResponse> unreadMentions(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(new MentionCountResponse(mentionService.taskUnseen(taskId, currentUser.getId())));
    }

    @PostMapping("/{taskId}/mentions/read")
    public ResponseEntity<Void> markMentionsRead(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        mentionService.markTaskSeen(taskId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser,
                                                       @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(taskId, currentUser.getId(), request));
    }

    @PutMapping("/{taskId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> editComment(@PathVariable Long taskId, @PathVariable Long commentId,
                                                        @AuthenticationPrincipal UserPrincipal currentUser,
                                                        @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.update(taskId, commentId, currentUser.getId(), request));
    }

    @DeleteMapping("/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long taskId, @PathVariable Long commentId,
                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        commentService.delete(taskId, commentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/activity")
    public ResponseEntity<List<TaskActivityResponse>> activity(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskActivityService.list(taskId, currentUser.getId(), taskAccessService));
    }

    @GetMapping("/{taskId}/unread")
    public ResponseEntity<UnreadResponse> unread(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(taskReadStateService.unread(taskId, currentUser.getId()));
    }

    @PostMapping("/{taskId}/read/chat")
    public ResponseEntity<Void> markChatRead(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        taskReadStateService.markChatRead(taskId, currentUser.getId()); return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/read/activity")
    public ResponseEntity<Void> markActivityRead(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal currentUser) {
        taskReadStateService.markActivityRead(taskId, currentUser.getId()); return ResponseEntity.noContent().build();
    }

    // Checkpoint endpoints under task
    @PostMapping("/{taskId}/checkpoints")
    public ResponseEntity<CheckpointResponse> addCheckpoint(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateCheckpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkpointService.addCheckpoint(taskId, currentUser.getId(), request));
    }

    @PatchMapping("/{taskId}/checkpoints/{checkpointId}/toggle")
    public ResponseEntity<CheckpointResponse> toggleCheckpoint(
            @PathVariable Long taskId,
            @PathVariable Long checkpointId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(checkpointService.toggleCheckpoint(taskId, checkpointId, currentUser.getId()));
    }

    @DeleteMapping("/{taskId}/checkpoints/{checkpointId}")
    public ResponseEntity<Void> deleteCheckpoint(
            @PathVariable Long taskId,
            @PathVariable Long checkpointId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        checkpointService.deleteCheckpoint(taskId, checkpointId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
