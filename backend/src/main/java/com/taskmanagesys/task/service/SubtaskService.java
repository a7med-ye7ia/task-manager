package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.dto.CreateSubtaskRequest;
import com.taskmanagesys.task.dto.SubtaskResponse;
import com.taskmanagesys.task.dto.UpdateSubtaskRequest;
import com.taskmanagesys.task.model.*;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import com.taskmanagesys.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SubtaskService {

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final SubtaskAssigneeRepository subtaskAssigneeRepository;
    private final TaskMemberRepository taskMemberRepository;
    private final UserRepository userRepository;
    private final TaskRulesService taskRulesService;
    private final TaskActivityService taskActivityService;
    private final TaskService taskService;
    private final TaskAccessService taskAccessService;
    private final NotificationService notifications;
    private final TaskFileService taskFileService;

    public SubtaskService(
            TaskRepository taskRepository,
            SubtaskRepository subtaskRepository,
            SubtaskAssigneeRepository subtaskAssigneeRepository,
            TaskMemberRepository taskMemberRepository,
            UserRepository userRepository,
            TaskRulesService taskRulesService,
            TaskActivityService taskActivityService,
            TaskService taskService, TaskAccessService taskAccessService, NotificationService notifications, TaskFileService taskFileService) {
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.subtaskAssigneeRepository = subtaskAssigneeRepository;
        this.taskMemberRepository = taskMemberRepository;
        this.userRepository = userRepository;
        this.taskRulesService = taskRulesService;
        this.taskActivityService = taskActivityService;
        this.taskService = taskService;
        this.taskAccessService = taskAccessService;
        this.notifications = notifications;
        this.taskFileService = taskFileService;
    }

    @Transactional
    public SubtaskResponse createSubtask(Long taskId, Long actorId, CreateSubtaskRequest request) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        taskRulesService.validateDeadlineNotInPast(request.getDeadline());

        // BR-14: Every subtask has at least one assignee
        if (request.getAssigneeUserIds() == null || request.getAssigneeUserIds().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "ASSIGNEE_REQUIRED", "Every subtask must have at least one assignee");
        }

        boolean hasOutsideMember = request.getAssigneeUserIds().stream().anyMatch(uid -> !taskMemberRepository.existsByTaskIdAndUserId(taskId, uid));
        if (hasOutsideMember && !request.isConfirmAddMember()) {
            throw new AppException(HttpStatus.CONFLICT, "MEMBER_CONFIRMATION_REQUIRED", "Assigning someone outside this task requires confirmation");
        }

        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(request.getTitle().trim());
        subtask.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        subtask.setDeadline(request.getDeadline());
        subtask.setStatus(SubtaskStatus.TODO);
        Subtask createdSubtask = subtaskRepository.save(subtask);

        // Assignees (BR-14)
        for (Long uid : request.getAssigneeUserIds()) {
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "User not found: " + uid));
            if (u.getStatus() != UserStatus.ACTIVE || u.getDeletedAt() != null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "Assignee must be an active user: " + u.getEmail());
            }

            // BR-14: Assigning a subtask to someone who is not yet on the task adds them to the task as a Member automatically
            if (!taskMemberRepository.existsByTaskIdAndUserId(taskId, uid)) {
                TaskMember tm = new TaskMember(task, u, TaskMemberRole.MEMBER, actor);
                taskMemberRepository.save(tm);
            }

            SubtaskAssignee sa = new SubtaskAssignee(subtask, u, actor);
            subtaskAssigneeRepository.save(sa);
        }

        // Recalculate deadline and task status
        List<Subtask> allSubtasks = subtaskRepository.findByTaskId(taskId);
        taskRulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, allSubtasks);
        TaskStatus nextStatus = taskRulesService.evaluateAutoTaskStatusOnSubtaskChange(task, allSubtasks);
        task.setStatus(nextStatus);
        taskRepository.save(task);

        taskActivityService.logActivity(taskId, actorId, "SUBTASK_CREATED", Map.of(
                "subtaskId", createdSubtask.getId(),
                "title", createdSubtask.getTitle(),
                "deadline", String.valueOf(createdSubtask.getDeadline()),
                "assignees", request.getAssigneeUserIds()
        ));

        for (Long uid : request.getAssigneeUserIds()) {
            notifications.subtaskAssigned(taskId, actorId, uid, createdSubtask.getTitle());
        }

        final Long createdSubtaskId = createdSubtask.getId();
        return taskService.getTaskDetails(taskId).getSubtasks().stream()
                .filter(s -> s.getId().equals(createdSubtaskId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public SubtaskResponse updateSubtask(Long taskId, Long subtaskId, Long actorId, UpdateSubtaskRequest request) {
        if (request.getStatus() != null) taskAccessService.requireSubtaskAssigneeOrLead(taskId, subtaskId, actorId); else taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found: " + subtaskId));

        if (!subtask.getTask().getId().equals(taskId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TASK_SUBTASK", "Subtask does not belong to specified task");
        }

        Map<String, Object> changes = new HashMap<>();

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            changes.put("old_title", subtask.getTitle());
            subtask.setTitle(request.getTitle().trim());
            changes.put("new_title", subtask.getTitle());
        }
        if (request.getDescription() != null) {
            changes.put("old_description", subtask.getDescription());
            subtask.setDescription(request.getDescription().trim());
            changes.put("new_description", subtask.getDescription());
        }
        if (request.getDeadline() != null) {
            taskRulesService.validateDeadlineNotInPast(request.getDeadline());
            changes.put("old_deadline", String.valueOf(subtask.getDeadline()));
            subtask.setDeadline(request.getDeadline());
            changes.put("new_deadline", String.valueOf(subtask.getDeadline()));
        }
        if (request.getStatus() != null && request.getStatus() != subtask.getStatus()) {
            changes.put("old_status", subtask.getStatus().name());
            subtask.setStatus(request.getStatus());
            changes.put("new_status", subtask.getStatus().name());
            if (subtask.getStatus() == SubtaskStatus.DONE) {
                taskMemberRepository.findByTaskIdAndRole(task.getId(), TaskMemberRole.LEAD).ifPresent(lead -> notifications.subtaskDone(task.getId(), actorId, lead.getUserId(), subtask.getTitle()));
                subtask.setDoneAt(Instant.now());
                taskActivityService.logActivity(taskId, actorId, "SUBTASK_DONE", Map.of("subtaskId", subtaskId, "subtaskTitle", subtask.getTitle(), "count", 1));
            } else {
                subtask.setDoneAt(null);
            }
        }

        if (request.getAssigneeUserIds() != null) {
            if (request.getAssigneeUserIds().isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "ASSIGNEE_REQUIRED", "Every subtask must have at least one assignee");
            boolean outside = request.getAssigneeUserIds().stream().anyMatch(uid -> !taskMemberRepository.existsByTaskIdAndUserId(taskId, uid));
            if (outside && !request.isConfirmAddMember()) throw new AppException(HttpStatus.CONFLICT, "MEMBER_CONFIRMATION_REQUIRED", "Assigning someone outside this task requires confirmation");
            subtaskAssigneeRepository.deleteBySubtaskId(subtaskId);
            for (Long uid : request.getAssigneeUserIds()) {
                User target = userRepository.findById(uid).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
                if (target.getStatus() != UserStatus.ACTIVE || target.getDeletedAt() != null) throw new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "User must be active");
                if (!taskMemberRepository.existsByTaskIdAndUserId(taskId, uid)) {
                    taskMemberRepository.save(new TaskMember(task, target, TaskMemberRole.MEMBER, userRepository.findById(actorId).orElseThrow()));
                    taskActivityService.logActivity(taskId, actorId, "MEMBER_ADDED", Map.of("userId", uid, "autoAddedForSubtask", subtaskId));
                }
                subtaskAssigneeRepository.save(new SubtaskAssignee(subtask, target, userRepository.findById(actorId).orElseThrow()));
            }
            changes.put("assignees", request.getAssigneeUserIds());
        }

        subtaskRepository.save(subtask);

        // Recalculate deadline and task status
        List<Subtask> allSubtasks = subtaskRepository.findByTaskId(taskId);
        taskRulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, allSubtasks);
        TaskStatus nextStatus = taskRulesService.evaluateAutoTaskStatusOnSubtaskChange(task, allSubtasks);
        TaskStatus oldTaskStatus = task.getStatus();
        task.setStatus(nextStatus);
        taskRepository.save(task);
        if (oldTaskStatus != nextStatus) {
            taskActivityService.logActivity(taskId, actorId, "TASK_STATUS_CHANGED", Map.of("old_status", oldTaskStatus.name(), "new_status", nextStatus.name(), "automatic", true));
            if (nextStatus == TaskStatus.REVIEW) notifyReviewers(task, actorId);
        }

        if (!changes.isEmpty()) {
            changes.put("subtaskId", subtaskId);
            taskActivityService.logActivity(taskId, actorId, "SUBTASK_EDITED", changes);
        }

        return taskService.getTaskDetails(taskId).getSubtasks().stream()
                .filter(s -> s.getId().equals(subtaskId))
                .findFirst()
                .orElseThrow();
    }

    private void notifyReviewers(Task task, Long actorId) {
        if (task.isSelfReported()) {
            notifications.selfReportedReview(task.getId(), actorId, task.getTitle());
            return;
        }
        var lead = taskMemberRepository.findByTaskIdAndRole(task.getId(), TaskMemberRole.LEAD);
        if (lead.isPresent()) notifications.taskReview(task.getId(), actorId, lead.get().getUserId(), task.getTitle());
        else userRepository.findByRoleAndStatusAndDeletedAtIsNull(com.taskmanagesys.user.model.Role.MANAGER, UserStatus.ACTIVE).stream().filter(u -> !Objects.equals(u.getId(), actorId)).forEach(u -> notifications.taskReview(task.getId(), actorId, u.getId(), task.getTitle()));
    }

    @Transactional
    public void deleteSubtask(Long taskId, Long subtaskId, Long actorId) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found: " + subtaskId));

        if (!subtask.getTask().getId().equals(taskId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TASK_SUBTASK", "Subtask does not belong to specified task");
        }

        taskFileService.deleteForSubtask(subtaskId);
        subtaskRepository.delete(subtask);

        // Recalculate task deadline and status
        List<Subtask> remainingSubtasks = subtaskRepository.findByTaskId(taskId);
        taskRulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, remainingSubtasks);
        TaskStatus nextStatus = taskRulesService.evaluateAutoTaskStatusOnSubtaskChange(task, remainingSubtasks);
        task.setStatus(nextStatus);
        taskRepository.save(task);

        taskActivityService.logActivity(taskId, actorId, "SUBTASK_DELETED", Map.of(
                "subtaskId", subtaskId,
                "title", subtask.getTitle()
        ));
    }

    @Transactional
    public SubtaskResponse assignUserToSubtask(Long taskId, Long subtaskId, Long targetUserId, Long actorId, boolean confirmAddMember) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found"));
        if (!subtask.getTask().getId().equals(taskId)) throw new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Subtask does not belong to this task");

        User actor = userRepository.findById(actorId).orElseThrow();
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Target user not found"));

        if (target.getStatus() != UserStatus.ACTIVE || target.getDeletedAt() != null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "User must be active");
        }

        if (!subtaskAssigneeRepository.existsBySubtaskIdAndUserId(subtaskId, targetUserId)) {
            // BR-14 auto-membership
            if (!taskMemberRepository.existsByTaskIdAndUserId(taskId, targetUserId)) {
                if (!confirmAddMember) throw new AppException(HttpStatus.CONFLICT, "MEMBER_CONFIRMATION_REQUIRED", "Assigning someone outside this task requires confirmation");
                TaskMember tm = new TaskMember(task, target, TaskMemberRole.MEMBER, actor);
                taskMemberRepository.save(tm);
                taskActivityService.logActivity(taskId, actorId, "MEMBER_ADDED", Map.of(
                        "userId", targetUserId,
                        "autoAddedForSubtask", subtaskId
                ));
            }

            SubtaskAssignee sa = new SubtaskAssignee(subtask, target, actor);
            subtaskAssigneeRepository.save(sa);

            taskActivityService.logActivity(taskId, actorId, "SUBTASK_ASSIGNEE_ADDED", Map.of(
                    "subtaskId", subtaskId,
                    "userId", targetUserId
            ));
        }

        return taskService.getTaskDetails(taskId).getSubtasks().stream()
                .filter(s -> s.getId().equals(subtaskId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public SubtaskResponse removeUserFromSubtask(Long taskId, Long subtaskId, Long targetUserId, Long actorId) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        taskService.getTaskEntity(taskId);
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found"));

        subtaskAssigneeRepository.deleteBySubtaskIdAndUserId(subtaskId, targetUserId);

        taskActivityService.logActivity(taskId, actorId, "SUBTASK_ASSIGNEE_REMOVED", Map.of(
                "subtaskId", subtaskId,
                "userId", targetUserId
        ));

        return taskService.getTaskDetails(taskId).getSubtasks().stream()
                .filter(s -> s.getId().equals(subtaskId))
                .findFirst()
                .orElseThrow();
    }
}
