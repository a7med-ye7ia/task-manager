package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.dto.CommentResponse;
import com.taskmanagesys.task.dto.CreateCommentRequest;
import com.taskmanagesys.task.dto.UpdateCommentRequest;
import com.taskmanagesys.task.model.Comment;
import com.taskmanagesys.task.model.Subtask;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.repository.CommentRepository;
import com.taskmanagesys.task.repository.CommentMentionRepository;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import com.taskmanagesys.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final UserRepository users;
    private final TaskAccessService access;
    private final TaskActivityService activity;
    private final CommentMentionRepository mentions;
    private final TaskMemberRepository members;
    private final NotificationService notifications;
    public CommentService(CommentRepository comments, TaskRepository tasks, SubtaskRepository subtasks,
                          UserRepository users, TaskAccessService access, TaskActivityService activity, CommentMentionRepository mentions, TaskMemberRepository members, NotificationService notifications) {
        this.comments = comments; this.tasks = tasks; this.subtasks = subtasks; this.users = users; this.access = access; this.activity = activity; this.mentions = mentions; this.members = members; this.notifications = notifications;
    }
    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, Long actorId) {
        requireCommentAccess(taskId, actorId);
        return comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(c -> response(c)).toList();
    }
    @Transactional
    public CommentResponse create(Long taskId, Long actorId, CreateCommentRequest request) {
        Task task = task(taskId); requireCommentAccess(taskId, actorId); writable(task);
        Comment comment = new Comment(); comment.setTask(task); comment.setAuthor(user(actorId));
        comment.setBody(request.getBody().trim());
        if (request.getSubtaskId() != null) {
            Subtask subtask = subtasks.findById(request.getSubtaskId()).orElseThrow(() -> missing("SUBTASK_NOT_FOUND", "Subtask not found"));
            if (!taskId.equals(subtask.getTask().getId())) throw missing("SUBTASK_NOT_FOUND", "Subtask not found");
            comment.setSubtask(subtask);
        }
        Comment saved = comments.save(comment); saveMentions(saved, taskId, actorId, request.getMentionedUserIds());
        activity.logActivity(taskId, actorId, "COMMENT_ADDED", java.util.Map.of("commentId", saved.getId(), "count", 1));
        return response(saved);
    }
    @Transactional
    public CommentResponse update(Long taskId, Long commentId, Long actorId, UpdateCommentRequest request) {
        Task task = task(taskId); requireCommentAccess(taskId, actorId); writable(task);
        Comment comment = comments.findByIdAndTaskId(commentId, taskId).orElseThrow(() -> missing("COMMENT_NOT_FOUND", "Comment not found"));
        if (comment.getDeletedAt() != null || comment.getAuthor() == null || !comment.getAuthor().getId().equals(actorId))
            throw new AppException(HttpStatus.FORBIDDEN, "COMMENT_EDIT_DENIED", "You can edit only your own comment");
        comment.setBody(request.getBody().trim()); comment.setEditedAt(Instant.now()); mentions.deleteByCommentId(commentId); saveMentions(comment, taskId, actorId, request.getMentionedUserIds());
        activity.logActivity(taskId, actorId, "COMMENT_EDITED", java.util.Map.of("commentId", commentId));
        return response(comment);
    }
    @Transactional
    public void delete(Long taskId, Long commentId, Long actorId) {
        Task task = task(taskId); requireCommentAccess(taskId, actorId); writable(task);
        Comment comment = comments.findByIdAndTaskId(commentId, taskId).orElseThrow(() -> missing("COMMENT_NOT_FOUND", "Comment not found"));
        if (!access.manager(actorId) && (comment.getAuthor() == null || !comment.getAuthor().getId().equals(actorId)))
            throw new AppException(HttpStatus.FORBIDDEN, "COMMENT_DELETE_DENIED", "You can delete only your own comment");
        comment.setDeletedAt(Instant.now());
        mentions.deleteByCommentId(commentId);
        activity.logActivity(taskId, actorId, "COMMENT_DELETED", java.util.Map.of("commentId", commentId));
    }
    private Task task(Long id) { return tasks.findById(id).orElseThrow(() -> missing("TASK_NOT_FOUND", "Task not found")); }
    private void requireCommentAccess(Long taskId, Long actorId) { if (!access.manager(actorId)) access.requireMember(taskId, actorId); }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> missing("USER_NOT_FOUND", "User not found")); }
    private void writable(Task task) { if (task.getStatus().name().equals("DONE")) throw new AppException(HttpStatus.CONFLICT, "COMMENTS_READ_ONLY", "Comments are read-only after completion"); }
    private AppException missing(String code, String message) { return new AppException(HttpStatus.NOT_FOUND, code, message); }
    private void saveMentions(Comment comment, Long taskId, Long actorId, java.util.List<Long> requestedIds) {
        if (requestedIds == null) return;
        for (Long id : requestedIds) {
            User target = users.findById(id).orElse(null);
            if (target == null || !members.existsByTaskIdAndUserId(taskId, id)) continue;
            String fullName = target.getFirstName() + " " + target.getLastName();
            if (java.util.regex.Pattern.compile("(?:^|\\s)@" + java.util.regex.Pattern.quote(fullName) + "(?!\\w)").matcher(comment.getBody()).find()) {
                mentions.save(new com.taskmanagesys.task.model.CommentMention(comment, target));
                notifications.mention(taskId, actorId, target.getId(), comment.getTask().getTitle(), comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName());
            }
        }
    }
    private java.util.List<Long> mentionIds(Comment comment) { return mentions.findByCommentId(comment.getId()).stream().map(m -> m.getUser().getId()).toList(); }
    private CommentResponse response(Comment comment) {
        var rows = mentions.findByCommentId(comment.getId());
        return CommentResponse.from(comment, rows.stream().map(m -> m.getUser().getId()).toList(), rows.stream().map(com.taskmanagesys.task.dto.MentionedUserResponse::from).toList());
    }
}
