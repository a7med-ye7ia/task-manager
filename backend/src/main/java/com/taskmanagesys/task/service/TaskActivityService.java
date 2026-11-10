package com.taskmanagesys.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.task.model.TaskActivity;
import com.taskmanagesys.task.dto.TaskActivityResponse;
import java.util.List;
import com.taskmanagesys.task.repository.TaskActivityRepository;
import com.taskmanagesys.history.service.HistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class TaskActivityService {

    private final TaskActivityRepository activityRepository;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskActivityService(TaskActivityRepository activityRepository, HistoryService historyService) {
        this.activityRepository = activityRepository;
        this.historyService = historyService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logActivity(Long taskId, Long actorId, String action, Map<String, Object> details) {
        String json = null;
        if (details != null && !details.isEmpty()) {
            try {
                json = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                json = "{}";
            }
        }
        TaskActivity activity = new TaskActivity(taskId, actorId, action, json);
        activityRepository.save(activity);
        historyService.recordTaskEvent(taskId, action, details);
    }

    @Transactional(readOnly = true)
    public List<TaskActivityResponse> list(Long taskId, Long actorId, TaskAccessService access) {
        access.requireMember(taskId, actorId);
        return activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(TaskActivityResponse::from).toList();
    }
}
