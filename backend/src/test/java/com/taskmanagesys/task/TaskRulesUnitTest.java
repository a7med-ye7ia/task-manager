package com.taskmanagesys.task;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.*;
import com.taskmanagesys.task.service.TaskRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRulesUnitTest {

    private TaskRulesService rulesService;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        rulesService = new TaskRulesService();
        today = LocalDate.now(TaskRulesService.CAIRO_ZONE);
    }

    @Test
    @DisplayName("BR-20: Deadline cannot be set in the past")
    void deadlineCannotBeInPast() {
        LocalDate pastDate = today.minusDays(1);
        assertThatThrownBy(() -> rulesService.validateDeadlineNotInPast(pastDate))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Deadline cannot be set in the past");

        // Today and future are valid
        rulesService.validateDeadlineNotInPast(today);
        rulesService.validateDeadlineNotInPast(today.plusDays(5));
    }

    @Test
    @DisplayName("BR-15: A task with no subtasks must have a deadline")
    void taskWithNoSubtasksRequiresDeadline() {
        Task task = new Task();
        task.setDeadline(null);

        assertThatThrownBy(() -> rulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, List.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Task with no subtasks must have a deadline");

        assertThatThrownBy(() -> rulesService.applyManualTaskDeadline(task, null, List.of()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("must have a deadline");
    }

    @Test
    @DisplayName("BR-16: Automatic task deadline equals latest subtask deadline when deadline_manual = false")
    void automaticTaskDeadlineFollowsLatestSubtask() {
        Task task = new Task();
        task.setDeadlineManual(false);

        Subtask s1 = new Subtask();
        s1.setDeadline(today.plusDays(3));

        Subtask s2 = new Subtask();
        s2.setDeadline(today.plusDays(7));

        rulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, List.of(s1, s2));
        assertThat(task.getDeadline()).isEqualTo(today.plusDays(7));

        // When subtask moving earlier
        s2.setDeadline(today.plusDays(2));
        rulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, List.of(s1, s2));
        assertThat(task.getDeadline()).isEqualTo(today.plusDays(3));
    }

    @Test
    @DisplayName("BR-17: Manual task deadline extension allowed only if >= latest subtask deadline")
    void manualTaskDeadlineRules() {
        Task task = new Task();
        task.setDeadlineManual(false);

        Subtask s1 = new Subtask();
        s1.setDeadline(today.plusDays(5));

        // Attempting to set task deadline earlier than latest subtask deadline throws error
        assertThatThrownBy(() -> rulesService.applyManualTaskDeadline(task, today.plusDays(4), List.of(s1)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("cannot be earlier than the latest subtask deadline");

        // Setting task deadline later than latest subtask deadline succeeds and sets deadlineManual = true
        rulesService.applyManualTaskDeadline(task, today.plusDays(10), List.of(s1));
        assertThat(task.getDeadline()).isEqualTo(today.plusDays(10));
        assertThat(task.isDeadlineManual()).isTrue();

        // Setting back to exactly the latest subtask deadline turns deadline_manual off
        rulesService.applyManualTaskDeadline(task, today.plusDays(5), List.of(s1));
        assertThat(task.getDeadline()).isEqualTo(today.plusDays(5));
        assertThat(task.isDeadlineManual()).isFalse();
    }

    @Test
    @DisplayName("BR-18: Subtask deadline extending past manual task deadline raises task deadline to match")
    void subtaskExceedingManualDeadlineRaisesTaskDeadline() {
        Task task = new Task();
        task.setDeadline(today.plusDays(8));
        task.setDeadlineManual(true);

        Subtask s1 = new Subtask();
        s1.setDeadline(today.plusDays(12));

        rulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, List.of(s1));

        assertThat(task.getDeadline()).isEqualTo(today.plusDays(12));
        assertThat(task.isDeadlineManual()).isTrue();
    }

    @Test
    @DisplayName("BR-19: Deleting last subtask retains current deadline as required field")
    void deletingLastSubtaskRetainsCurrentDeadline() {
        Task task = new Task();
        task.setDeadline(today.plusDays(10));
        task.setDeadlineManual(false);

        // Subtasks list now empty
        rulesService.recalculateTaskDeadlinesOnSubtasksChanged(task, List.of());
        assertThat(task.getDeadline()).isEqualTo(today.plusDays(10));
    }

    @Test
    @DisplayName("BR-21: Automatic task status transitions on subtask status changes")
    void statusFlowTransitions() {
        Task task = new Task();
        task.setStatus(TaskStatus.TODO);

        Subtask s1 = new Subtask();
        s1.setStatus(SubtaskStatus.TODO);
        Subtask s2 = new Subtask();
        s2.setStatus(SubtaskStatus.TODO);

        // When s1 leaves TODO -> task becomes IN_PROGRESS
        s1.setStatus(SubtaskStatus.IN_PROGRESS);
        TaskStatus nextStatus = rulesService.evaluateAutoTaskStatusOnSubtaskChange(task, List.of(s1, s2));
        assertThat(nextStatus).isEqualTo(TaskStatus.IN_PROGRESS);

        // When both subtasks DONE -> task moves to REVIEW
        task.setStatus(TaskStatus.IN_PROGRESS);
        s1.setStatus(SubtaskStatus.DONE);
        s2.setStatus(SubtaskStatus.DONE);
        nextStatus = rulesService.evaluateAutoTaskStatusOnSubtaskChange(task, List.of(s1, s2));
        assertThat(nextStatus).isEqualTo(TaskStatus.REVIEW);

        // A TODO task with one DONE and one unfinished subtask starts, but does not review.
        task.setStatus(TaskStatus.TODO);
        s1.setStatus(SubtaskStatus.DONE);
        s2.setStatus(SubtaskStatus.IN_PROGRESS);
        nextStatus = rulesService.evaluateAutoTaskStatusOnSubtaskChange(task, List.of(s1, s2));
        assertThat(nextStatus).isEqualTo(TaskStatus.IN_PROGRESS);

        // REVIEW is not changed by the automatic evaluator when a subtask is reopened.
        task.setStatus(TaskStatus.REVIEW);
        nextStatus = rulesService.evaluateAutoTaskStatusOnSubtaskChange(task, List.of(s1, s2));
        assertThat(nextStatus).isEqualTo(TaskStatus.REVIEW);
    }

    @Test
    @DisplayName("BR-22: Task progress calculation")
    void taskProgressCalculation() {
        // Case 1: Subtasks present -> DONE / total
        Subtask s1 = new Subtask();
        s1.setStatus(SubtaskStatus.DONE);
        Subtask s2 = new Subtask();
        s2.setStatus(SubtaskStatus.IN_PROGRESS);
        Subtask s3 = new Subtask();
        s3.setStatus(SubtaskStatus.TODO);

        int progress = rulesService.calculateTaskProgress(TaskStatus.IN_PROGRESS, List.of(s1, s2, s3), List.of());
        // 1/3 = 33%
        assertThat(progress).isEqualTo(33);

        s2.setStatus(SubtaskStatus.DONE);
        progress = rulesService.calculateTaskProgress(TaskStatus.IN_PROGRESS, List.of(s1, s2, s3), List.of());
        // 2/3 = 67%
        assertThat(progress).isEqualTo(67);

        // Case 2: No subtasks, but checkpoints present -> ticked / total
        Checkpoint c1 = new Checkpoint();
        c1.setDone(true);
        Checkpoint c2 = new Checkpoint();
        c2.setDone(false);

        progress = rulesService.calculateTaskProgress(TaskStatus.IN_PROGRESS, List.of(), List.of(c1, c2));
        // 1/2 = 50%
        assertThat(progress).isEqualTo(50);

        // Case 3: Neither subtasks nor checkpoints
        progress = rulesService.calculateTaskProgress(TaskStatus.TODO, List.of(), List.of());
        assertThat(progress).isEqualTo(0);

        progress = rulesService.calculateTaskProgress(TaskStatus.REVIEW, List.of(), List.of());
        assertThat(progress).isEqualTo(100);

        progress = rulesService.calculateTaskProgress(TaskStatus.DONE, List.of(), List.of());
        assertThat(progress).isEqualTo(100);
    }

    @Test
    @DisplayName("BR-23: Subtask progress calculation")
    void subtaskProgressCalculation() {
        // With checkpoints
        Checkpoint c1 = new Checkpoint();
        c1.setDone(true);
        Checkpoint c2 = new Checkpoint();
        c2.setDone(true);
        Checkpoint c3 = new Checkpoint();
        c3.setDone(false);

        int progress = rulesService.calculateSubtaskProgress(SubtaskStatus.IN_PROGRESS, List.of(c1, c2, c3));
        // 2/3 = 67%
        assertThat(progress).isEqualTo(67);

        // Without checkpoints: 0% if not DONE, 100% if DONE
        progress = rulesService.calculateSubtaskProgress(SubtaskStatus.IN_PROGRESS, List.of());
        assertThat(progress).isEqualTo(0);

        progress = rulesService.calculateSubtaskProgress(SubtaskStatus.DONE, List.of());
        assertThat(progress).isEqualTo(100);
    }

    @Test
    @DisplayName("BR-20: isOverdue helper")
    void isOverdueHelper() {
        assertThat(rulesService.isOverdue(today.minusDays(1), TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(rulesService.isOverdue(today.minusDays(1), TaskStatus.DONE)).isFalse();
        assertThat(rulesService.isOverdue(today, TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(rulesService.isOverdue(today.plusDays(1), TaskStatus.IN_PROGRESS)).isFalse();
    }
}
