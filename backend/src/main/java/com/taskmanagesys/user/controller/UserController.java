package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.history.dto.UserReportResponse;
import com.taskmanagesys.history.service.ReportService;
import com.taskmanagesys.task.dto.TaskResponse;
import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.service.TaskService;
import com.taskmanagesys.user.dto.PublicProfileResponse;
import com.taskmanagesys.user.dto.UserDto;
import com.taskmanagesys.user.dto.UserTaskSummaryDto;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.service.UserAdminService;
import com.taskmanagesys.user.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserAdminService userAdminService;
    private final TaskService taskService;
    private final TaskMemberRepository taskMemberRepository;
    private final ReportService reportService;

    public UserController(
            UserService userService,
            UserAdminService userAdminService,
            TaskService taskService,
            TaskMemberRepository taskMemberRepository,
            ReportService reportService) {
        this.userService = userService;
        this.userAdminService = userAdminService;
        this.taskService = taskService;
        this.taskMemberRepository = taskMemberRepository;
        this.reportService = reportService;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserDto> getUserDetails(@PathVariable("id") Long id) {
        UserDto user = userAdminService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}/public")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable("id") Long id) {
        var user = userAdminService.getUserEntity(id);
        boolean active = user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE;
        return ResponseEntity.ok(new PublicProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getRole(), user.getAvatarPath(), active));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<UserTaskSummaryDto>> getUserTasks(
            @PathVariable("id") Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // Confirm user exists
        userAdminService.getUser(id);

        List<TaskResponse> tasks = taskService.getTasksForUser(id);

        if (from != null && to != null) {
            if (to.isBefore(from)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "'to' date cannot be before 'from' date");
            }
            ZoneId cairo = ZoneId.of("Africa/Cairo");
            Instant rangeStart = from.atStartOfDay(cairo).toInstant();
            Instant rangeEnd = to.plusDays(1).atStartOfDay(cairo).toInstant();
            tasks = tasks.stream().filter(t -> {
                boolean createdBeforeEnd = t.getCreatedAt() == null || t.getCreatedAt().isBefore(rangeEnd);
                boolean notDoneBeforeStart = t.getDoneAt() == null || !t.getDoneAt().isBefore(rangeStart);
                return createdBeforeEnd && notDoneBeforeStart;
            }).collect(Collectors.toList());
        }

        List<UserTaskSummaryDto> summaries = tasks.stream().map(task -> {
            Optional<TaskMember> memberOpt = taskMemberRepository.findByTaskIdAndUserId(task.getId(), id);
            TaskMemberRole role = memberOpt.map(TaskMember::getRole).orElse(TaskMemberRole.MEMBER);
            return new UserTaskSummaryDto(
                    task.getId(),
                    task.getTitle(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDeadline(),
                    task.isOverdue(),
                    role,
                    task.getProgress()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}/public/tasks")
    public ResponseEntity<List<UserTaskSummaryDto>> getPublicUserTasks(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal viewer) {
        userAdminService.getUser(id);
        List<TaskResponse> tasks = taskService.getTasksForUser(id);

        if (from != null && to != null) {
            if (to.isBefore(from)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "'to' date cannot be before 'from' date");
            }
            ZoneId cairo = ZoneId.of("Africa/Cairo");
            Instant rangeStart = from.atStartOfDay(cairo).toInstant();
            Instant rangeEnd = to.plusDays(1).atStartOfDay(cairo).toInstant();
            tasks = tasks.stream().filter(t -> {
                boolean createdBeforeEnd = t.getCreatedAt() == null || t.getCreatedAt().isBefore(rangeEnd);
                boolean notDoneBeforeStart = t.getDoneAt() == null || !t.getDoneAt().isBefore(rangeStart);
                return createdBeforeEnd && notDoneBeforeStart;
            }).collect(Collectors.toList());
        }

        java.util.Set<Long> viewerTasks = taskMemberRepository.findByUserId(viewer.getId()).stream().map(TaskMember::getTaskId).collect(Collectors.toSet());
        List<UserTaskSummaryDto> summaries = tasks.stream().map(task -> {
            Optional<TaskMember> memberOpt = taskMemberRepository.findByTaskIdAndUserId(task.getId(), id);
            TaskMemberRole role = memberOpt.map(TaskMember::getRole).orElse(TaskMemberRole.MEMBER);
            return new UserTaskSummaryDto(task.getId(), task.getTitle(), task.getStatus(), task.getPriority(), task.getDeadline(), task.isOverdue(), role, task.getProgress());
        }).filter(t -> !filter.equalsIgnoreCase("LEADS") || t.getUserRole() == TaskMemberRole.LEAD)
          .filter(t -> !filter.equalsIgnoreCase("COMMON") || viewerTasks.contains(t.getId())).toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<UserReportResponse> getUserReport(
            @PathVariable("id") Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal viewer) {
        return ResponseEntity.ok(reportService.generateReport(viewer.getId(), id, from, to));
    }
}
