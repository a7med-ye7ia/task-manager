package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.Checkpoint;
import com.taskmanagesys.task.model.Subtask;
import com.taskmanagesys.task.model.SubtaskStatus;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TaskRulesService {

    public static final ZoneId CAIRO_ZONE = ZoneId.of("Africa/Cairo");

    /**
     * BR-20: Deadline cannot be set in the past.
     * Evaluated against Cairo time today.
     */
    public void validateDeadlineNotInPast(LocalDate deadline) {
        if (deadline == null) {
            return;
        }
        LocalDate todayCairo = LocalDate.now(CAIRO_ZONE);
        if (deadline.isBefore(todayCairo)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEADLINE", "Deadline cannot be set in the past");
        }
    }

    /**
     * BR-15, BR-16, BR-17, BR-18, BR-19:
     * Calculates / synchronizes task deadline according to its subtasks.
     */
    public void recalculateTaskDeadlinesOnSubtasksChanged(Task task, List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            // BR-19: If the last subtask is deleted, the task keeps its current deadline, which is now an ordinary required field.
            if (task.getDeadline() == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "DEADLINE_REQUIRED", "Task with no subtasks must have a deadline");
            }
            return;
        }

        LocalDate latestSubtaskDeadline = subtasks.stream()
                .map(Subtask::getDeadline)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (latestSubtaskDeadline == null) {
            return;
        }

        if (!task.isDeadlineManual()) {
            // BR-16: By default (deadline_manual = false) the task deadline equals the latest subtask deadline
            // and is recalculated whenever a subtask is added, changed or deleted, including moving earlier.
            task.setDeadline(latestSubtaskDeadline);
        } else {
            // BR-18: If a subtask is saved with a deadline later than a manual task deadline,
            // the task deadline is raised to match it and stays manual.
            if (task.getDeadline() != null && latestSubtaskDeadline.isAfter(task.getDeadline())) {
                task.setDeadline(latestSubtaskDeadline);
            }
        }
    }

    /**
     * BR-17: A Manager or Lead can give the task extra time by setting a deadline later than
     * the latest subtask deadline. This sets deadline_manual = true and the date stays fixed.
     * A deadline earlier than the latest subtask deadline is rejected with a clear message.
     * Setting it back to exactly the latest subtask deadline turns deadline_manual off.
     */
    public void applyManualTaskDeadline(Task task, LocalDate newDeadline, List<Subtask> subtasks) {
        validateDeadlineNotInPast(newDeadline);

        if (subtasks == null || subtasks.isEmpty()) {
            // BR-15: A task with no subtasks must have a deadline.
            if (newDeadline == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "DEADLINE_REQUIRED", "A task with no subtasks must have a deadline");
            }
            task.setDeadline(newDeadline);
            task.setDeadlineManual(false);
            return;
        }

        LocalDate latestSubtaskDeadline = subtasks.stream()
                .map(Subtask::getDeadline)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (latestSubtaskDeadline != null) {
            if (newDeadline == null || newDeadline.isBefore(latestSubtaskDeadline)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEADLINE",
                        "Task deadline cannot be earlier than the latest subtask deadline (" + latestSubtaskDeadline + ")");
            }

            if (newDeadline.isEqual(latestSubtaskDeadline)) {
                task.setDeadline(newDeadline);
                task.setDeadlineManual(false);
            } else {
                task.setDeadline(newDeadline);
                task.setDeadlineManual(true);
            }
        } else {
            task.setDeadline(newDeadline);
        }
    }

    /**
     * BR-21: Status flow transitions for Task.
     */
    public TaskStatus evaluateAutoTaskStatusOnSubtaskChange(Task task, List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            return task.getStatus();
        }

        boolean anyLeftTodo = subtasks.stream().anyMatch(s -> s.getStatus() != SubtaskStatus.TODO);
        boolean allDone = subtasks.stream().allMatch(s -> s.getStatus() == SubtaskStatus.DONE);

        // A TODO task whose only/last subtask is completed goes directly to REVIEW.
        if (task.getStatus() == TaskStatus.TODO && allDone) return TaskStatus.REVIEW;
        // The first subtask leaving TODO starts the task.
        if (task.getStatus() == TaskStatus.TODO && anyLeftTodo) return TaskStatus.IN_PROGRESS;
        // Completing the last subtask sends an active task to review.
        if (task.getStatus() == TaskStatus.IN_PROGRESS && allDone) return TaskStatus.REVIEW;

        // REVIEW and DONE are not changed by this automatic evaluator.

        return task.getStatus();
    }

    /**
     * BR-22: Task progress calculation.
     * Task progress = DONE subtasks divided by all subtasks.
     * With no subtasks but with checkpoints, it is ticked checkpoints divided by all checkpoints.
     * With neither it is 0%, or 100% when the task is in REVIEW or DONE. Round to a whole percent.
     */
    public int calculateTaskProgress(TaskStatus status, List<Subtask> subtasks, List<Checkpoint> taskCheckpoints) {
        if (subtasks != null && !subtasks.isEmpty()) {
            long doneCount = subtasks.stream().filter(s -> s.getStatus() == SubtaskStatus.DONE).count();
            return (int) Math.round((doneCount * 100.0) / subtasks.size());
        }

        if (taskCheckpoints != null && !taskCheckpoints.isEmpty()) {
            long doneCount = taskCheckpoints.stream().filter(Checkpoint::isDone).count();
            return (int) Math.round((doneCount * 100.0) / taskCheckpoints.size());
        }

        if (status == TaskStatus.REVIEW || status == TaskStatus.DONE) {
            return 100;
        }

        return 0;
    }

    /**
     * BR-23: Subtask progress calculation.
     * A subtask's progress comes from its checkpoints if it has any, otherwise 0% or 100% (DONE).
     */
    public int calculateSubtaskProgress(SubtaskStatus status, List<Checkpoint> subtaskCheckpoints) {
        if (subtaskCheckpoints != null && !subtaskCheckpoints.isEmpty()) {
            long doneCount = subtaskCheckpoints.stream().filter(Checkpoint::isDone).count();
            return (int) Math.round((doneCount * 100.0) / subtaskCheckpoints.size());
        }

        return status == SubtaskStatus.DONE ? 100 : 0;
    }

    /**
     * Helper to check if a task is overdue (Cairo date is past deadline and status != DONE)
     */
    public boolean isOverdue(LocalDate deadline, TaskStatus status) {
        if (deadline == null || status == TaskStatus.DONE) {
            return false;
        }
        LocalDate todayCairo = LocalDate.now(CAIRO_ZONE);
        return deadline.isBefore(todayCairo);
    }
}
