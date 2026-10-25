package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.dto.CheckpointResponse;
import com.taskmanagesys.task.dto.CreateCheckpointRequest;
import com.taskmanagesys.task.model.Checkpoint;
import com.taskmanagesys.task.model.Subtask;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.CheckpointRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class CheckpointService {

    private final CheckpointRepository checkpointRepository;
    private final SubtaskRepository subtaskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final TaskActivityService taskActivityService;
    private final TaskAccessService taskAccessService;

    public CheckpointService(
            CheckpointRepository checkpointRepository,
            SubtaskRepository subtaskRepository,
            UserRepository userRepository,
            TaskService taskService,
            TaskActivityService taskActivityService, TaskAccessService taskAccessService) {
        this.checkpointRepository = checkpointRepository;
        this.subtaskRepository = subtaskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.taskActivityService = taskActivityService;
        this.taskAccessService = taskAccessService;
    }

    @Transactional
    public CheckpointResponse addCheckpoint(Long taskId, Long actorId, CreateCheckpointRequest request) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        Subtask subtask = null;
        if (request.getSubtaskId() != null) {
            subtask = subtaskRepository.findById(request.getSubtaskId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found"));
            if (!subtask.getTask().getId().equals(taskId)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TASK_SUBTASK", "Subtask does not belong to specified task");
            }
        }

        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTask(task);
        checkpoint.setSubtask(subtask);
        checkpoint.setTitle(request.getTitle().trim());
        checkpoint = checkpointRepository.save(checkpoint);

        taskActivityService.logActivity(taskId, actorId, "CHECKPOINT_ADDED", Map.of(
                "checkpointId", checkpoint.getId(),
                "title", checkpoint.getTitle(),
                "subtaskId", subtask != null ? subtask.getId() : "TASK_LEVEL"
        ));

        return taskService.mapCheckpointToResponse(checkpoint);
    }

    @Transactional
    public CheckpointResponse toggleCheckpoint(Long taskId, Long checkpointId, Long actorId) {
        Checkpoint accessCheckpoint = checkpointRepository.findById(checkpointId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "CHECKPOINT_NOT_FOUND", "Checkpoint not found"));
        if (!accessCheckpoint.getTask().getId().equals(taskId)) throw new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "You do not have permission to perform this action on this task");
        if (accessCheckpoint.getSubtask() == null) taskAccessService.requireMember(taskId, actorId); else taskAccessService.requireSubtaskAssigneeOrLead(taskId, accessCheckpoint.getSubtask().getId(), actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "CHECKPOINT_NOT_FOUND", "Checkpoint not found"));

        if (!checkpoint.getTask().getId().equals(taskId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TASK_CHECKPOINT", "Checkpoint does not belong to specified task");
        }

        boolean willBeDone = !checkpoint.isDone();
        checkpoint.setDone(willBeDone);
        if (willBeDone) {
            checkpoint.setDoneBy(actor);
            checkpoint.setDoneAt(Instant.now());
        } else {
            checkpoint.setDoneBy(null);
            checkpoint.setDoneAt(null);
        }

        checkpoint = checkpointRepository.save(checkpoint);

        taskActivityService.logActivity(taskId, actorId, willBeDone ? "CHECKPOINT_CHECKED" : "CHECKPOINT_UNCHECKED", Map.of(
                "checkpointId", checkpoint.getId(),
                "title", checkpoint.getTitle(),
                "isDone", willBeDone
        ));

        return taskService.mapCheckpointToResponse(checkpoint);
    }

    @Transactional
    public void deleteCheckpoint(Long taskId, Long checkpointId, Long actorId) {
        taskAccessService.requireManagerOrLead(taskId, actorId);
        Task task = taskService.getTaskEntity(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TASK_DONE_READONLY", "A task in DONE is read-only until restored");
        }

        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "CHECKPOINT_NOT_FOUND", "Checkpoint not found"));

        if (!checkpoint.getTask().getId().equals(taskId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TASK_CHECKPOINT", "Checkpoint does not belong to specified task");
        }

        taskActivityService.logActivity(taskId, actorId, "CHECKPOINT_DELETED", Map.of(
                "checkpointId", checkpoint.getId(),
                "title", checkpoint.getTitle()
        ));

        checkpointRepository.delete(checkpoint);
    }
}
