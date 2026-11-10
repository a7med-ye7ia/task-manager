package com.taskmanagesys.history.service;

import com.taskmanagesys.history.model.WorkHistory;
import com.taskmanagesys.history.model.WorkHistoryEvent;
import com.taskmanagesys.history.repository.WorkHistoryEventRepository;
import com.taskmanagesys.history.repository.WorkHistoryRepository;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class HistoryService {
    private final WorkHistoryRepository histories;
    private final WorkHistoryEventRepository events;
    private final TaskRepository tasks;
    private final TaskMemberRepository members;
    private final Clock clock;

    public HistoryService(WorkHistoryRepository histories, WorkHistoryEventRepository events,
                          TaskRepository tasks, TaskMemberRepository members, Clock clock) {
        this.histories = histories; this.events = events; this.tasks = tasks; this.members = members; this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long taskId, Long actorId, String eventType, String description, String oldValue, String newValue) {
        Map<String, Object> details = new HashMap<>();
        if (description != null) details.put("description", description);
        if (oldValue != null) details.put("oldValue", oldValue);
        if (newValue != null) details.put("newValue", newValue);
        if (actorId != null) details.put("actorId", actorId);
        recordTaskEvent(taskId, eventType, details);
        if ("STATUS_CHANGE".equals(eventType) && "DONE".equals(newValue)) {
            recordTaskEvent(taskId, "TASK_DONE", details);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTaskEvent(Long taskId, String eventType, Map<String, Object> details) {
        Task task = tasks.findById(taskId).orElse(null);
        if (task == null) return;
        Instant now = Instant.now(clock);
        if ("MEMBER_REMOVED".equals(eventType) && details != null && details.get("userId") != null) {
            Long removedUserId = Long.valueOf(details.get("userId").toString());
            histories.findByUserIdAndTaskId(removedUserId, taskId).ifPresent(history -> { history.setLeftAt(now); history.setFinalStatus(task.getStatus().name()); histories.save(history); });
        }
        for (TaskMember member : members.findByTaskId(taskId)) {
            WorkHistory history = histories.findByUserIdAndTaskId(member.getUserId(), taskId)
                    .orElseGet(() -> createHistory(task, member, now));
            history.setFinalStatus(task.getStatus().name());
            histories.save(history);
            String subtaskTitle = details == null ? null : value(details, "subtaskTitle");
            Integer count = details == null ? null : number(details, "count");
            events.save(new WorkHistoryEvent(history.getId(), eventType, subtaskTitle, count, now));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTaskDeleted(Task task, Long actorId) {
        Instant now = Instant.now(clock);
        for (TaskMember member : members.findByTaskId(task.getId())) {
            WorkHistory history = histories.findByUserIdAndTaskId(member.getUserId(), task.getId())
                    .orElseGet(() -> createHistory(task, member, now));
            history.setFinalStatus(task.getStatus().name()); history.setLeftAt(now); histories.save(history);
            events.save(new WorkHistoryEvent(history.getId(), "TASK_DELETED", null, null, now));
        }
    }

    @Transactional
    public long cleanup(Instant cutoff) {
        events.deleteByOccurredAtBefore(cutoff);
        return histories.deleteByJoinedAtBefore(cutoff);
    }

    private WorkHistory createHistory(Task task, TaskMember member, Instant now) {
        WorkHistory history = new WorkHistory();
        history.setUserId(member.getUserId()); history.setTaskId(task.getId()); history.setTaskTitle(task.getTitle());
        history.setPriority(task.getPriority().name()); history.setDeadline(task.getDeadline()); history.setTaskRole(member.getRole().name());
        history.setJoinedAt(member.getAddedAt() == null ? now : member.getAddedAt()); history.setSelfReported(task.isSelfReported()); history.setFinalStatus(task.getStatus().name());
        return histories.save(history);
    }
    private String value(Map<String, Object> details, String key) { Object value = details.get(key); return value == null ? null : value.toString(); }
    private Integer number(Map<String, Object> details, String key) { Object value = details.get(key); return value instanceof Number n ? n.intValue() : null; }
}
