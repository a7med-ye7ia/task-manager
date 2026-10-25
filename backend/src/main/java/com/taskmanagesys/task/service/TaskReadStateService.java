package com.taskmanagesys.task.service;

import com.taskmanagesys.task.dto.UnreadResponse;
import com.taskmanagesys.task.model.TaskActivity;
import com.taskmanagesys.task.model.TaskReadState;
import com.taskmanagesys.task.repository.CommentRepository;
import com.taskmanagesys.task.repository.CommentMentionRepository;
import com.taskmanagesys.task.repository.TaskActivityRepository;
import com.taskmanagesys.task.repository.TaskReadStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskReadStateService {
    private final TaskReadStateRepository states; private final CommentRepository comments; private final TaskActivityRepository activities; private final CommentMentionRepository mentions; private final TaskAccessService access;
    public TaskReadStateService(TaskReadStateRepository states, CommentRepository comments, TaskActivityRepository activities, CommentMentionRepository mentions, TaskAccessService access) { this.states = states; this.comments = comments; this.activities = activities; this.mentions = mentions; this.access = access; }
    @Transactional(readOnly = true)
    public UnreadResponse unread(Long taskId, Long userId) {
        access.requireMember(taskId, userId); TaskReadState state = states.findByIdTaskIdAndIdUserId(taskId, userId).orElseGet(() -> new TaskReadState(taskId, userId));
        long chat = comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().filter(c -> c.getId() > state.getChatLastId() && (c.getAuthor() == null || !userId.equals(c.getAuthor().getId()))).count();
        long activity = activities.findByTaskIdOrderByCreatedAtDesc(taskId).stream().filter(a -> a.getId() > state.getActivityLastId() && (a.getActorId() == null || !userId.equals(a.getActorId()))).count();
        return new UnreadResponse(chat > 0, activity > 0, chat, activity);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> unreadCounts(Collection<Long> taskIds, Long userId) {
        if (taskIds == null || taskIds.isEmpty()) return Map.of();
        Map<Long, TaskReadState> stateByTask = states.findAllById(taskIds.stream().map(id -> new TaskReadState.Id(id, userId)).toList())
                .stream().collect(Collectors.toMap(s -> s.getId().getTaskId(), Function.identity()));
        Map<Long, Long> counts = new HashMap<>();
        comments.findByTaskIdInOrderByIdAsc(taskIds).forEach(comment -> {
            TaskReadState state = stateByTask.get(comment.getTask().getId());
            long lastId = state == null ? 0L : state.getChatLastId();
            if (comment.getId() > lastId && (comment.getAuthor() == null || !userId.equals(comment.getAuthor().getId()))) {
                counts.merge(comment.getTask().getId(), 1L, Long::sum);
            }
        });
        activities.findByTaskIdInOrderByIdAsc(taskIds).forEach(activity -> {
            TaskReadState state = stateByTask.get(activity.getTaskId());
            long lastId = state == null ? 0L : state.getActivityLastId();
            if (activity.getId() > lastId && (activity.getActorId() == null || !userId.equals(activity.getActorId()))) {
                counts.merge(activity.getTaskId(), 1L, Long::sum);
            }
        });
        mentions.findUnseenByUserAndTaskIds(userId, taskIds).stream()
                .filter(mention -> mention.getUser() == null || userId.equals(mention.getUser().getId()))
                .forEach(mention -> counts.merge(mention.getComment().getTask().getId(), 1L, Long::sum));
        return counts;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> unreadMentionCounts(Collection<Long> taskIds, Long userId) {
        if (taskIds == null || taskIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        mentions.findUnseenByUserAndTaskIds(userId, taskIds).stream()
                .filter(mention -> mention.getUser() == null || userId.equals(mention.getUser().getId()))
                .forEach(mention -> counts.merge(mention.getComment().getTask().getId(), 1L, Long::sum));
        return counts;
    }
    @Transactional
    public void markChatRead(Long taskId, Long userId) { access.requireMember(taskId, userId); TaskReadState state = state(taskId, userId); comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(c -> c.getId()).max(Long::compareTo).ifPresent(state::setChatLastId); states.save(state); }
    @Transactional
    public void markActivityRead(Long taskId, Long userId) { access.requireMember(taskId, userId); TaskReadState state = state(taskId, userId); activities.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(TaskActivity::getId).max(Long::compareTo).ifPresent(state::setActivityLastId); states.save(state); }
    private TaskReadState state(Long taskId, Long userId) { return states.findByIdTaskIdAndIdUserId(taskId, userId).orElseGet(() -> states.save(new TaskReadState(taskId, userId))); }
}
