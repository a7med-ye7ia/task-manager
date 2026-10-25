package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.dto.*;
import com.taskmanagesys.task.model.*;
import com.taskmanagesys.task.repository.*;
import com.taskmanagesys.user.dto.UserSummary;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.audit.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMemberRepository taskMemberRepository;
    private final SubtaskRepository subtaskRepository;
    private final SubtaskAssigneeRepository subtaskAssigneeRepository;
    private final CheckpointRepository checkpointRepository;
    private final UserRepository userRepository;
    private final TaskRulesService taskRulesService;
    private final TaskActivityService taskActivityService;
    private final TaskAccessService taskAccessService;
    private final CommentRepository commentRepository;
    private final NotificationService notifications;
    private final CommentMentionRepository commentMentionRepository;
    private final AuditLogService auditLogService;
    private final TaskReadStateService taskReadStateService;

    public TaskService(
            TaskRepository taskRepository,
            TaskMemberRepository taskMemberRepository,
            SubtaskRepository subtaskRepository,
            SubtaskAssigneeRepository subtaskAssigneeRepository,
            CheckpointRepository checkpointRepository,
            UserRepository userRepository,
            TaskRulesService taskRulesService,
            TaskActivityService taskActivityService, TaskAccessService taskAccessService, CommentRepository commentRepository, NotificationService notifications, CommentMentionRepository commentMentionRepository, AuditLogService auditLogService, TaskReadStateService taskReadStateService) {
        this.taskRepository = taskRepository;
        this.taskMemberRepository = taskMemberRepository;
        this.subtaskRepository = subtaskRepository;
        this.subtaskAssigneeRepository = subtaskAssigneeRepository;
        this.checkpointRepository = checkpointRepository;
        this.userRepository = userRepository;
        this.taskRulesService = taskRulesService;
        this.taskActivityService = taskActivityService;
        this.taskAccessService = taskAccessService;
        this.commentRepository = commentRepository;
        this.notifications = notifications;
        this.commentMentionRepository = commentMentionRepository;
        this.auditLogService = auditLogService;
        this.taskReadStateService = taskReadStateService;
    }

    @Transactional
    public TaskResponse createTask(Long actorId, CreateTaskRequest request) {
        taskAccessService.requireManager(actorId);
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        List<CreateSubtaskRequest> draftSubtasks = request.getSubtasks() == null ? List.of() : request.getSubtasks();
        // BR-15: only a task with no subtasks requires a supplied deadline.
        if (draftSubtasks.isEmpty() && request.getDeadline() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DEADLINE_REQUIRED", "Task deadline is required");
        }
        if (request.getDeadline() != null) taskRulesService.validateDeadlineNotInPast(request.getDeadline());
        for (CreateSubtaskRequest draft : draftSubtasks) taskRulesService.validateDeadlineNotInPast(draft.getDeadline());

        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        LocalDate latestSubtaskDeadline = draftSubtasks.stream().map(CreateSubtaskRequest::getDeadline).max(LocalDate::compareTo).orElse(null);
        task.setDeadline(latestSubtaskDeadline == null ? request.getDeadline() : (request.getDeadline() != null && request.getDeadline().isAfter(latestSubtaskDeadline) ? request.getDeadline() : latestSubtaskDeadline));
        task.setDeadlineManual(false);
        task.setPriority(request.getPriority());
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy(actor);
        task = taskRepository.save(task);

        // Members (BR-24)
        Set<Long> memberIds = new HashSet<>(request.getMemberUserIds());
        if (request.getLeadUserId() != null) {
            memberIds.add(request.getLeadUserId());
        }

        for (Long uid : memberIds) {
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "Selected employee not found: " + uid));
            if (u.getStatus() != UserStatus.ACTIVE || u.getDeletedAt() != null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "Employee must be active: " + u.getEmail());
            }
            TaskMemberRole role = Objects.equals(uid, request.getLeadUserId()) ? TaskMemberRole.LEAD : TaskMemberRole.MEMBER;
            TaskMember tm = new TaskMember(task, u, role, actor);
            taskMemberRepository.save(tm);
        }

        // Draft subtasks and checkpoints are saved in this same transaction.
        for (CreateSubtaskRequest draft : draftSubtasks) {
            if (draft.getAssigneeUserIds() == null || draft.getAssigneeUserIds().isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "ASSIGNEE_REQUIRED", "Every subtask must have at least one assignee");
            }
            Subtask subtask = new Subtask();
            subtask.setTask(task); subtask.setTitle(draft.getTitle().trim());
            subtask.setDescription(draft.getDescription() == null ? null : draft.getDescription().trim());
            subtask.setDeadline(draft.getDeadline()); subtask.setStatus(SubtaskStatus.TODO);
            subtask = subtaskRepository.save(subtask);
            for (Long userId : new HashSet<>(draft.getAssigneeUserIds())) {
                User assignee = userRepository.findById(userId).orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "Selected employee not found: " + userId));
                if (assignee.getStatus() != UserStatus.ACTIVE || assignee.getDeletedAt() != null) throw new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_ACTIVE", "Assignee must be active");
                if (!taskMemberRepository.existsByTaskIdAndUserId(task.getId(), userId)) taskMemberRepository.save(new TaskMember(task, assignee, TaskMemberRole.MEMBER, actor));
                subtaskAssigneeRepository.save(new SubtaskAssignee(subtask, assignee, actor));
            }
        }
        if (request.getCheckpoints() != null) for (CreateCheckpointRequest draft : request.getCheckpoints()) {
            Checkpoint checkpoint = new Checkpoint(); checkpoint.setTask(task); checkpoint.setTitle(draft.getTitle().trim()); checkpointRepository.save(checkpoint);
        }

        taskActivityService.logActivity(task.getId(), actorId, "TASK_CREATED", Map.of(
                "title", task.getTitle(),
                "priority", task.getPriority().name(),
                "deadline", String.valueOf(task.getDeadline())
        ));

        return getTaskDetails(task.getId());
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, Long actorId, UpdateTaskRequest request) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = getTaskEntity(taskId);

        // A task in DONE is read-only until restored (BR-25)
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        Map<String, Object> changes = new HashMap<>();

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            changes.put("old_title", task.getTitle());
            task.setTitle(request.getTitle().trim());
            changes.put("new_title", task.getTitle());
        }
        if (request.getDescription() != null) {
            changes.put("old_description", task.getDescription());
            task.setDescription(request.getDescription().trim());
            changes.put("new_description", task.getDescription());
        }
        if (request.getPriority() != null) {
            changes.put("old_priority", task.getPriority().name());
            task.setPriority(request.getPriority());
            changes.put("new_priority", task.getPriority().name());
        }
        if (request.getDeadline() != null) {
            List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);
            taskRulesService.applyManualTaskDeadline(task, request.getDeadline(), subtasks);
            changes.put("new_deadline", String.valueOf(task.getDeadline()));
            changes.put("deadline_manual", task.isDeadlineManual());
        }

        task = taskRepository.save(task);

        if (!changes.isEmpty()) {
            taskActivityService.logActivity(taskId, actorId, "TASK_EDITED", changes);
        }

        return getTaskDetails(taskId);
    }

    @Transactional
    public TaskResponse changeStatus(Long taskId, Long actorId, ChangeTaskStatusRequest request) {
        Task taskForAccess = getTaskEntity(taskId);
        if (taskForAccess.getStatus() == TaskStatus.DONE) {
            taskAccessService.requireManagerOrLead(taskId, actorId);
            throw new AppException(HttpStatus.CONFLICT, "TASK_ARCHIVED", "Archived tasks can only be restored");
        }
        if (request.getStatus() == TaskStatus.DONE || taskForAccess.getStatus() == TaskStatus.DONE || (taskForAccess.getStatus() == TaskStatus.REVIEW && request.getStatus() == TaskStatus.IN_PROGRESS)) taskAccessService.requireManagerOrLead(taskId, actorId); else taskAccessService.requireMember(taskId, actorId);
        Task task = taskForAccess;
        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return getTaskDetails(taskId);
        }

        List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);

        // BR-21 & BR-30 rules:
        // REVIEW -> DONE: Lead or Manager only. Starts 30-day archive.
        // REVIEW -> IN_PROGRESS: Lead/Manager, with a required comment giving reason.
        if (oldStatus == TaskStatus.REVIEW && newStatus == TaskStatus.IN_PROGRESS) {
            if (request.getReason() == null || request.getReason().trim().length() < 3) {
                throw new AppException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED",
                        "A reason comment is required when sending a task back to IN_PROGRESS");
            }
        }

        // BR-21: A task with subtasks cannot be moved to REVIEW by hand (must be automatic when all subtasks DONE)
        if (!subtasks.isEmpty() && newStatus == TaskStatus.REVIEW) {
            boolean allDone = subtasks.stream().allMatch(s -> s.getStatus() == SubtaskStatus.DONE);
            if (!allDone) {
                throw new AppException(HttpStatus.BAD_REQUEST, "SUBTASKS_NOT_DONE",
                        "A task with subtasks cannot be moved to REVIEW until every subtask is DONE");
            }
        }

        // BR-30 & BR-31
        if (newStatus == TaskStatus.DONE) {
            task.setDoneAt(Instant.now());
            task.setPurgeAt(Instant.now().plus(java.time.Duration.ofDays(30)));
            task.setPurgeWarningSentAt(null);
        } else if (oldStatus == TaskStatus.DONE) {
            // Restore within 30 days
            task.setDoneAt(null);
            task.setPurgeAt(null);
            task.setPurgeWarningSentAt(null);
        }

        task.setStatus(newStatus);
        taskRepository.save(task);

        Map<String, Object> details = new HashMap<>();
        details.put("old_status", oldStatus.name());
        details.put("new_status", newStatus.name());
        if (request.getReason() != null) {
            details.put("reason", request.getReason());
        }
        taskActivityService.logActivity(taskId, actorId, "TASK_STATUS_CHANGED", details);
        if (newStatus == TaskStatus.DONE) notifications.taskDone(taskId, actorId, task.getTitle());
        if (newStatus == TaskStatus.REVIEW) {
            var lead = taskMemberRepository.findByTaskIdAndRole(taskId, TaskMemberRole.LEAD);
            if (lead.isPresent()) notifications.taskReview(taskId, actorId, lead.get().getUserId(), task.getTitle());
            else userRepository.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, UserStatus.ACTIVE).stream().filter(u -> !Objects.equals(u.getId(), actorId)).forEach(u -> notifications.taskReview(taskId, actorId, u.getId(), task.getTitle()));
        }
        if (oldStatus == TaskStatus.REVIEW && newStatus == TaskStatus.IN_PROGRESS) notifications.taskSentBack(taskId, actorId, task.getTitle());
        if (oldStatus == TaskStatus.REVIEW && newStatus == TaskStatus.IN_PROGRESS) {
            Comment reasonComment = new Comment();
            reasonComment.setTask(task);
            reasonComment.setAuthor(userRepository.findById(actorId).orElseThrow());
            reasonComment.setBody(request.getReason().trim());
            commentRepository.save(reasonComment);
        }

        return getTaskDetails(taskId);
    }

    @Transactional
    public TaskResponse sendToReview(Long taskId, Long actorId) {
        Task task = getTaskEntity(taskId);
        taskAccessService.requireMember(taskId, actorId);
        if (task.getStatus() != TaskStatus.TODO && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only TODO or IN_PROGRESS tasks can be sent to review");
        }
        List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);
        if (subtasks.stream().anyMatch(s -> s.getStatus() != SubtaskStatus.DONE)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SUBTASKS_NOT_DONE", "Every subtask must be DONE before review");
        }
        TaskStatus old = task.getStatus();
        task.setStatus(TaskStatus.REVIEW);
        taskRepository.save(task);
        taskActivityService.logActivity(taskId, actorId, "TASK_SENT_TO_REVIEW", Map.of("old_status", old.name()));
        notifyReviewers(task, actorId);
        return getTaskDetails(taskId);
    }

    @Transactional(readOnly = true)
    public Page<MemberCandidateResponse> getMemberCandidates(Long taskId, Long actorId, String query, Pageable pageable) {
        getTaskEntity(taskId);
        taskAccessService.requireManagerOrLead(taskId, actorId);
        return userRepository.findMemberCandidates(taskId, query == null ? "" : query.trim(), pageable);
    }

    @Transactional
    public TaskResponse sendBack(Long taskId, Long actorId, SendBackTaskRequest request) {
        Task task = getTaskEntity(taskId);
        taskAccessService.requireManagerOrLead(taskId, actorId);
        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only tasks in review can be sent back");
        }
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < 3) throw new AppException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED", "A reason comment is required");
        Set<Long> chosenSubtasks = new HashSet<>(request.getSubtaskIds());
        Set<Long> chosenCheckpoints = new HashSet<>(request.getCheckpointIds());
        List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);
        for (Subtask subtask : subtasks) {
            if (chosenSubtasks.contains(subtask.getId())) {
                if (subtask.getStatus() != SubtaskStatus.DONE) throw new AppException(HttpStatus.BAD_REQUEST, "SUBTASK_NOT_DONE", "Only DONE subtasks can be reopened");
                subtask.setStatus(SubtaskStatus.TODO);
                subtask.setDoneAt(null);
                subtaskRepository.save(subtask);
                taskActivityService.logActivity(taskId, actorId, "SUBTASK_REOPENED", Map.of("subtaskId", subtask.getId()));
            }
        }
        if (chosenSubtasks.stream().anyMatch(id -> subtasks.stream().noneMatch(s -> Objects.equals(s.getId(), id)))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SUBTASK_NOT_FOUND", "Selected subtask does not belong to this task");
        }
        List<Checkpoint> checkpoints = checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(taskId);
        for (Checkpoint checkpoint : checkpoints) {
            if (chosenCheckpoints.contains(checkpoint.getId())) {
                if (!checkpoint.isDone()) throw new AppException(HttpStatus.BAD_REQUEST, "CHECKPOINT_NOT_DONE", "Only ticked checkpoints can be reopened");
                checkpoint.setDone(false);
                checkpoint.setDoneBy(null);
                checkpoint.setDoneAt(null);
                checkpointRepository.save(checkpoint);
                taskActivityService.logActivity(taskId, actorId, "CHECKPOINT_REOPENED", Map.of("checkpointId", checkpoint.getId()));
            }
        }
        if (chosenCheckpoints.stream().anyMatch(id -> checkpoints.stream().noneMatch(c -> Objects.equals(c.getId(), id)))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "CHECKPOINT_NOT_FOUND", "Selected checkpoint does not belong to this task");
        }
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(userRepository.findById(actorId).orElseThrow());
        comment.setBody(reason);
        commentRepository.save(comment);
        taskActivityService.logActivity(taskId, actorId, "TASK_SENT_BACK", Map.of("reason", reason, "subtaskIds", chosenSubtasks, "checkpointIds", chosenCheckpoints));
        notifications.taskSentBack(taskId, actorId, task.getTitle(), reason);
        return getTaskDetails(taskId);
    }

    @Transactional
    public TaskResponse restoreTask(Long taskId, Long actorId, TaskStatus targetStatus) {
        Task task = getTaskEntity(taskId);
        taskAccessService.requireManagerOrLead(taskId, actorId);
        if (task.getStatus() != TaskStatus.DONE) throw new AppException(HttpStatus.CONFLICT, "TASK_NOT_ARCHIVED", "Only archived tasks can be restored");
        if (task.getPurgeAt() != null && task.getPurgeAt().isBefore(Instant.now())) throw new AppException(HttpStatus.CONFLICT, "TASK_PURGE_EXPIRED", "This archived task can no longer be restored");
        if (targetStatus == null || targetStatus == TaskStatus.DONE) throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_RESTORE_STATUS", "Restore status must be TODO, IN_PROGRESS or REVIEW");
        task.setDoneAt(null); task.setPurgeAt(null); task.setPurgeWarningSentAt(null); task.setStatus(targetStatus); taskRepository.save(task);
        taskActivityService.logActivity(taskId, actorId, "TASK_RESTORED", Map.of("status", targetStatus.name()));
        taskMemberRepository.findByTaskId(taskId).stream().map(TaskMember::getUserId).distinct().forEach(id -> notifications.taskRestored(taskId, actorId, id, task.getTitle()));
        return getTaskDetails(taskId);
    }

    private void notifyReviewers(Task task, Long actorId) {
        var lead = taskMemberRepository.findByTaskIdAndRole(task.getId(), TaskMemberRole.LEAD);
        if (lead.isPresent()) notifications.taskReview(task.getId(), actorId, lead.get().getUserId(), task.getTitle());
        else userRepository.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, UserStatus.ACTIVE).stream().filter(u -> !Objects.equals(u.getId(), actorId)).forEach(u -> notifications.taskReview(task.getId(), actorId, u.getId(), task.getTitle()));
    }

    @Transactional
    public void deleteTask(Long taskId, Long actorId) {
        taskAccessService.requireManager(actorId);
        Task task = getTaskEntity(taskId);
          auditLogService.record(actorId, null, "TASK_DELETED", Map.of(
                  "entityType", "TASK",
                  "entityId", task.getId(),
                  "taskId", task.getId(),
                  "title", task.getTitle(),
                  "actorId", actorId));
        // Cascades remove subtasks, members, checkpoints, activities
        taskRepository.delete(task);
          taskRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(t -> mapToResponse(t, subtaskRepository.findByTaskId(t.getId()), checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(t.getId()), taskMemberRepository.findByTaskId(t.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForUser(Long userId) {
        return taskRepository.findTasksForUser(userId).stream()
                .map(t -> mapToResponse(t, subtaskRepository.findByTaskId(t.getId()), checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(t.getId()), taskMemberRepository.findByTaskId(t.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTaskPool(Long userId, String search, TaskStatus status, TaskPriority priority, java.time.LocalDate deadlineFrom, java.time.LocalDate deadlineTo, boolean myTasks, boolean iLead, boolean archived, String sort, Pageable pageable) {
        List<Task> source = taskRepository.findAll();
        source = source.stream().filter(task -> archived ? task.getStatus() == TaskStatus.DONE : task.getStatus() != TaskStatus.DONE).filter(task -> search == null || search.isBlank() || task.getTitle().toLowerCase().contains(search.toLowerCase())).filter(task -> status == null || task.getStatus() == status).filter(task -> priority == null || task.getPriority() == priority).filter(task -> deadlineFrom == null || !task.getDeadline().isBefore(deadlineFrom)).filter(task -> deadlineTo == null || !task.getDeadline().isAfter(deadlineTo)).filter(task -> !myTasks || taskMemberRepository.existsByTaskIdAndUserId(task.getId(), userId) || subtaskRepository.findByTaskId(task.getId()).stream().anyMatch(s -> subtaskAssigneeRepository.existsBySubtaskIdAndUserId(s.getId(), userId))).filter(task -> !iLead || taskMemberRepository.findByTaskIdAndUserId(task.getId(), userId).map(m -> m.getRole() == TaskMemberRole.LEAD).orElse(false)).sorted((a,b) -> { if ("priority".equalsIgnoreCase(sort)) return a.getPriority().compareTo(b.getPriority()); if ("created".equalsIgnoreCase(sort)) return b.getCreatedAt().compareTo(a.getCreatedAt()); return a.getDeadline().compareTo(b.getDeadline()); }).toList();
        int start = Math.min((int) pageable.getOffset(), source.size()); int end = Math.min(start + pageable.getPageSize(), source.size());
        List<TaskResponse> content = source.subList(start, end).stream().map(t -> {
            TaskResponse response = mapToResponse(t, subtaskRepository.findByTaskId(t.getId()), checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(t.getId()), taskMemberRepository.findByTaskId(t.getId()));
            return response;
        }).toList();
        Map<Long, Long> unreadCounts = taskReadStateService.unreadCounts(content.stream().map(TaskResponse::getId).toList(), userId);
        Map<Long, Long> mentionCounts = taskReadStateService.unreadMentionCounts(content.stream().map(TaskResponse::getId).toList(), userId);
        content.forEach(response -> {
            response.setUnreadCount(unreadCounts.getOrDefault(response.getId(), 0L));
            response.setMentionUnreadCount(mentionCounts.getOrDefault(response.getId(), 0L));
        });
        return new PageImpl<>(content, pageable, source.size());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskDetails(Long taskId) {
        Task task = getTaskEntity(taskId);
        List<Subtask> subtasks = subtaskRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        List<Checkpoint> taskCheckpoints = checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(taskId);
        List<TaskMember> members = taskMemberRepository.findByTaskId(taskId);
        return mapToResponse(task, subtasks, taskCheckpoints, members);
    }

    @Transactional
    public TaskResponse addMember(Long taskId, Long actorId, Long userId) {
        taskAccessService.requireManagerOrLead(taskId, actorId); Task task = getTaskEntity(taskId); ensureWritable(task);
        if (!taskMemberRepository.existsByTaskIdAndUserId(taskId, userId)) { User user = userRepository.findById(userId).orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "User not found")); taskMemberRepository.save(new TaskMember(task, user, TaskMemberRole.MEMBER, userRepository.getReferenceById(actorId))); taskActivityService.logActivity(taskId, actorId, "MEMBER_ADDED", Map.of("userId", userId)); notifications.taskAssigned(taskId, actorId, userId, task.getTitle()); }
        return getTaskDetails(taskId);
    }
    @Transactional
    public TaskResponse removeMember(Long taskId, Long actorId, Long userId) {
        taskAccessService.requireManagerOrLead(taskId, actorId); ensureWritable(getTaskEntity(taskId)); if (taskMemberRepository.findByTaskIdAndUserId(taskId, userId).map(m -> m.getRole() == TaskMemberRole.LEAD).orElse(false)) throw new AppException(HttpStatus.BAD_REQUEST, "LEAD_MUST_CHANGE", "Change the Lead before removing them");
        for (Subtask subtask : subtaskRepository.findByTaskId(taskId)) if (subtaskAssigneeRepository.existsBySubtaskIdAndUserId(subtask.getId(), userId)) subtaskAssigneeRepository.deleteBySubtaskIdAndUserId(subtask.getId(), userId);
        taskMemberRepository.deleteByTaskIdAndUserId(taskId, userId); taskActivityService.logActivity(taskId, actorId, "MEMBER_REMOVED", Map.of("userId", userId)); return getTaskDetails(taskId);
    }
    @Transactional
    public TaskResponse changeLead(Long taskId, Long actorId, Long userId) {
        taskAccessService.requireManager(actorId); Task task = getTaskEntity(taskId); ensureWritable(task); User actor = userRepository.getReferenceById(actorId);
        taskMemberRepository.findByTaskIdAndRole(taskId, TaskMemberRole.LEAD).ifPresent(old -> { old.setRole(TaskMemberRole.MEMBER); taskMemberRepository.save(old); });
        TaskMember member = taskMemberRepository.findByTaskIdAndUserId(taskId, userId).orElseGet(() -> taskMemberRepository.save(new TaskMember(task, userRepository.getReferenceById(userId), TaskMemberRole.MEMBER, actor))); member.setRole(TaskMemberRole.LEAD); taskMemberRepository.save(member); taskActivityService.logActivity(taskId, actorId, "LEAD_CHANGED", Map.of("userId", userId)); return getTaskDetails(taskId);
    }

    public Task getTaskEntity(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found with ID: " + taskId));
    }

    private void ensureWritable(Task task) { if (task.getStatus() == TaskStatus.DONE) throw new AppException(HttpStatus.CONFLICT, "TASK_ARCHIVED", "Archived tasks are read-only until restored"); }

    public TaskResponse mapToResponse(Task task, List<Subtask> subtasks, List<Checkpoint> taskCheckpoints, List<TaskMember> members) {
        TaskResponse res = new TaskResponse();
        res.setId(task.getId());
        res.setTitle(task.getTitle());
        res.setDescription(task.getDescription());
        res.setDeadline(task.getDeadline());
        res.setDeadlineManual(task.isDeadlineManual());
        res.setOverdue(taskRulesService.isOverdue(task.getDeadline(), task.getStatus()));
        res.setPriority(task.getPriority());
        res.setStatus(task.getStatus());
        res.setDoneAt(task.getDoneAt());
          res.setPurgeAt(task.getPurgeAt());
        res.setCreatedAt(task.getCreatedAt());
        res.setUpdatedAt(task.getUpdatedAt());

        if (task.getCreatedBy() != null) {
            res.setCreatedBy(UserSummary.from(task.getCreatedBy()));
        }

        // Members & Lead
        List<TaskMemberDto> memberDtos = new ArrayList<>();
        UserSummary leadDto = null;
        for (TaskMember tm : members) {
            UserSummary uDto = UserSummary.from(tm.getUser());
            memberDtos.add(new TaskMemberDto(uDto, tm.getRole()));
            if (tm.getRole() == TaskMemberRole.LEAD) {
                leadDto = uDto;
            }
        }
        res.setMembers(memberDtos);
        res.setLead(leadDto);

        // Subtasks
        List<SubtaskResponse> subtaskResponses = new ArrayList<>();
        for (Subtask s : subtasks) {
            SubtaskResponse sr = new SubtaskResponse();
            sr.setId(s.getId());
            sr.setTaskId(task.getId());
            sr.setTitle(s.getTitle());
            sr.setDescription(s.getDescription());
            sr.setDeadline(s.getDeadline());
            sr.setStatus(s.getStatus());
            sr.setDoneAt(s.getDoneAt());
            sr.setCreatedAt(s.getCreatedAt());

            List<Checkpoint> subtaskCps = checkpointRepository.findBySubtaskIdOrderByPositionAscIdAsc(s.getId());
            sr.setProgress(taskRulesService.calculateSubtaskProgress(s.getStatus(), subtaskCps));
            sr.setCheckpoints(subtaskCps.stream().map(this::mapCheckpointToResponse).collect(Collectors.toList()));

            List<SubtaskAssignee> assignees = subtaskAssigneeRepository.findBySubtaskId(s.getId());
            sr.setAssignees(assignees.stream().map(a -> UserSummary.from(a.getUser())).collect(Collectors.toList()));

            subtaskResponses.add(sr);
        }
        res.setSubtasks(subtaskResponses);

        // Task-level checkpoints
        res.setCheckpoints(taskCheckpoints.stream().map(this::mapCheckpointToResponse).collect(Collectors.toList()));

        // Progress (BR-22)
        res.setProgress(taskRulesService.calculateTaskProgress(task.getStatus(), subtasks, taskCheckpoints));

        return res;
    }

    public CheckpointResponse mapCheckpointToResponse(Checkpoint cp) {
        CheckpointResponse cpr = new CheckpointResponse();
        cpr.setId(cp.getId());
        cpr.setTaskId(cp.getTask().getId());
        cpr.setSubtaskId(cp.getSubtask() != null ? cp.getSubtask().getId() : null);
        cpr.setTitle(cp.getTitle());
        cpr.setDone(cp.isDone());
        cpr.setPosition(cp.getPosition());
        if (cp.getDoneBy() != null) {
            cpr.setDoneBy(UserSummary.from(cp.getDoneBy()));
        }
        return cpr;
    }
}
