package com.taskmanagesys.notification.service;

import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.model.Notification;
import com.taskmanagesys.notification.repository.NotificationRepository;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final TaskMemberRepository members;
    private final UserRepository users;
    private final ManagerEmailPrefsRepository prefs;
    private final EmailOutboxService outbox;
    private final TaskRepository tasks;

    public NotificationService(
            NotificationRepository notifications,
            TaskMemberRepository members,
            UserRepository users,
            ManagerEmailPrefsRepository prefs,
            EmailOutboxService outbox,
            TaskRepository tasks
    ) {
        this.notifications = notifications;
        this.members = members;
        this.users = users;
        this.prefs = prefs;
        this.outbox = outbox;
        this.tasks = tasks;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllReadByUserId(userId);
    }

    @Transactional
    public int clearAll(Long userId) {
        return notifications.deleteAllByUserId(userId);
    }

    @Transactional
    public void accountEmail(Long userId, String type, String template, String message, Map<String,Object> payload) {
        notifyOne(userId, type, null, message, true, template, payload);
    }

    @Transactional
    public void accountEmailTo(String address, Long userId, String type, String template, String message, Map<String,Object> payload) {
        notifications.save(new Notification(userId, type, null, message));
        outbox.queueEmail(address, template, payload);
    }

    @Transactional
    public void signupRequest(User applicant) {
        users.findManagersForSignupNotification().forEach(manager ->
                notifyOne(manager.getId(), "SIGNUP_REQUEST", null, "A new sign-up request is waiting for approval.", true, "NEW_SIGNUP_REQUEST", Map.of("name", applicant.getFirstName()+" "+applicant.getLastName(), "email", applicant.getEmail()))
        );
    }

    @Transactional
    public void mention(Long taskId, Long actorId, Long userId, String taskTitle, String authorName) {
        if (!Objects.equals(actorId, userId)) {
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("title", taskTitle);
            data.put("authorName", authorName);
            data.put("actorName", authorName);
            notifyOne(userId, "MENTION", taskId, authorName + " mentioned you in “" + taskTitle + "”.", true, "mention", data);
        }
    }

    @Transactional
    public void taskAssigned(Long taskId, Long actorId, Long userId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("title", title);
        resolveActorName(actorId).ifPresent(name -> data.put("actorName", name));
        notifyOne(userId, "TASK_ASSIGNED", taskId, "You were added to “" + title + "”.", true, "task-assigned", data);
    }

    @Transactional
    public void subtaskAssigned(Long taskId, Long actorId, Long userId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("title", title);
        data.put("subtaskTitle", title);
        resolveActorName(actorId).ifPresent(name -> data.put("actorName", name));
        notifyOne(userId, "SUBTASK_ASSIGNED", taskId, "You were assigned to subtask “" + title + "”.", true, "subtask-assigned", data);
    }

    @Transactional
    public void subtaskDone(Long taskId, Long actorId, Long userId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("title", title);
        data.put("subtaskTitle", title);
        resolveActorName(actorId).ifPresent(name -> data.put("actorName", name));
        notifyOne(userId, "SUBTASK_DONE", taskId, "Subtask “" + title + "” was completed.", true, "subtask-done", data);
    }

    @Transactional
    public void taskReview(Long taskId, Long actorId, Long userId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("title", title);
        resolveActorName(actorId).ifPresent(name -> data.put("actorName", name));
        notifyOne(userId, "TASK_REVIEW", taskId, "Task “" + title + "” is waiting for review.", true, "task-review", data);
    }

    @Transactional
    public void taskRestored(Long taskId, Long actorId, Long userId, String title) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("title", title);
        resolveActorName(actorId).ifPresent(name -> data.put("actorName", name));
        notifyOne(userId, "TASK_RESTORED", taskId, "Task “" + title + "” was restored.", false, "task-restored", data);
    }

    @Transactional
    public void taskDone(Long taskId, Long actorId, String title) {
        String actorName = resolveActorName(actorId).orElse("A manager");
        members.findByTaskId(taskId).stream()
                .map(TaskMember::getUserId)
                .filter(id -> !Objects.equals(id, actorId))
                .distinct()
                .forEach(id -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("taskId", taskId);
                    data.put("title", title);
                    data.put("actorName", actorName);
                    notifyOne(id, "TASK_DONE", taskId, "Task “" + title + "” was completed.", false, "task-done", data);
                });
    }

    @Transactional
    public void taskSentBack(Long taskId, Long actorId, String title) {
        taskSentBack(taskId, actorId, title, null);
    }

    @Transactional
    public void taskSentBack(Long taskId, Long actorId, String title, String reason) {
        String actorName = resolveActorName(actorId).orElse("A manager");
        members.findByTaskId(taskId).stream()
                .map(TaskMember::getUserId)
                .filter(id -> !Objects.equals(id, actorId))
                .distinct()
                .forEach(id -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("taskId", taskId);
                    data.put("title", title);
                    data.put("reason", reason == null ? "" : reason);
                    data.put("actorName", actorName);
                    notifyOne(id, "TASK_SENT_BACK", taskId, "Task “" + title + "” was sent back." + (reason == null || reason.isBlank() ? "" : " Reason: " + reason), false, "task-sent-back", data);
                });
    }

    @Transactional
    public void leadVacant(Long taskId, Long actorId, String title) {
        users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, com.taskmanagesys.user.model.UserStatus.ACTIVE).stream()
                .map(User::getId)
                .filter(id -> !Objects.equals(id, actorId))
                .forEach(id -> notifyOne(id, "LEAD_VACANT", taskId, "Task “" + title + "” no longer has a Lead.", false, "lead-vacant", Map.of("taskId", taskId, "title", title)));
    }

    @Transactional
    public void archiveWarning(Long taskId, Long actorId, Long leadId, String title) {
        Map<String, Object> data = Map.of("taskId", taskId, "title", title);
        if (leadId != null) {
            notifyOne(leadId, "ARCHIVE_WARNING", taskId, "Task “" + title + "” will be archived soon.", true, "archive-warning", data);
        } else {
            users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, com.taskmanagesys.user.model.UserStatus.ACTIVE)
                    .forEach(u -> notifyOne(u.getId(), "ARCHIVE_WARNING", taskId, "Task “" + title + "” will be archived soon.", true, "archive-warning", data));
        }
    }

    @Transactional
    public void selfReportedCreated(Long taskId, Long creatorId, String creatorName, String taskTitle) {
        users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, com.taskmanagesys.user.model.UserStatus.ACTIVE)
                .stream()
                .map(User::getId)
                .filter(id -> !Objects.equals(id, creatorId))
                .forEach(id -> notifyOne(id, "SELF_REPORTED_CREATED", taskId,
                        creatorName + " created self-reported task “" + taskTitle + "”.",
                        true, "self-reported-created", Map.of("taskId", taskId, "title", taskTitle, "creatorName", creatorName, "actorName", creatorName)));
    }

    @Transactional
    public void selfReportedReview(Long taskId, Long actorId, String taskTitle) {
        String actorName = resolveActorName(actorId).orElse("The creator");
        users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, com.taskmanagesys.user.model.UserStatus.ACTIVE)
                .stream()
                .map(User::getId)
                .filter(id -> !Objects.equals(id, actorId))
                .forEach(id -> notifyOne(id, "SELF_REPORTED_REVIEW", taskId,
                        "Self-reported task “" + taskTitle + "” is waiting for review.",
                        true, "self-reported-review", Map.of("taskId", taskId, "title", taskTitle, "actorName", actorName)));
    }

    private Optional<String> resolveActorName(Long actorId) {
        if (actorId == null) return Optional.empty();
        return users.findById(actorId).map(u -> u.getFirstName() + " " + u.getLastName());
    }

    private void notifyOne(Long userId, String type, Long taskId, String message, boolean email, String template, Map<String,Object> payload) {
        notifications.save(new Notification(userId, type, taskId, message));
        if (!email) return;
        users.findById(userId).ifPresent(u -> {
            ManagerEmailPrefs p = u.getRole() == Role.MANAGER ? prefs.findById(userId).orElse(null) : null;
            boolean allowed = true;
            if (p != null) {
                allowed = p.isEmailsEnabled();
                if (type.equals("SIGNUP_REQUEST")) allowed = allowed && p.isOnSignupRequest();
                if (type.equals("TASK_DONE") || type.equals("TASK_REVIEW")) allowed = allowed && p.isOnTaskCompleted();
                if (type.equals("ARCHIVE_WARNING")) allowed = allowed && p.isOnArchiveWarning();
                if (type.equals("SELF_REPORTED_CREATED")) allowed = allowed && p.isOnSelfReported();
                if (type.equals("SELF_REPORTED_REVIEW")) allowed = allowed && (p.isOnSelfReported() || p.isOnTaskCompleted());
            }
            if (allowed) {
                Map<String, Object> enriched = new HashMap<>(payload != null ? payload : Map.of());
                if (taskId != null) {
                    tasks.findById(taskId).ifPresent(t -> {
                        enriched.putIfAbsent("taskId", t.getId());
                        enriched.putIfAbsent("title", t.getTitle());
                        enriched.putIfAbsent("taskTitle", t.getTitle());
                        if (t.getPriority() != null) {
                            enriched.putIfAbsent("priority", t.getPriority().name());
                        }
                        if (t.getDeadline() != null) {
                            enriched.putIfAbsent("deadline", t.getDeadline().toString());
                        }
                    });
                }
                outbox.queueEmail(u.getEmail(), template, enriched);
            }
        });
    }
}
