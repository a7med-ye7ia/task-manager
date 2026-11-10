package com.taskmanagesys.task;

import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.history.service.HistoryService;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.dto.ChangeTaskStatusRequest;
import com.taskmanagesys.task.dto.CreateTaskRequest;
import com.taskmanagesys.task.dto.SendBackTaskRequest;
import com.taskmanagesys.task.dto.TaskResponse;
import com.taskmanagesys.task.model.*;
import com.taskmanagesys.task.repository.*;
import com.taskmanagesys.task.service.*;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfReportedTaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskMemberRepository taskMemberRepository;
    @Mock private SubtaskRepository subtaskRepository;
    @Mock private SubtaskAssigneeRepository subtaskAssigneeRepository;
    @Mock private CheckpointRepository checkpointRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRulesService taskRulesService;
    @Mock private TaskActivityService taskActivityService;
    @Mock private TaskAccessService taskAccessService;
    @Mock private CommentRepository commentRepository;
    @Mock private NotificationService notifications;
    @Mock private CommentMentionRepository commentMentionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TaskReadStateService taskReadStateService;
    @Mock private HistoryService historyService;
    @Mock private TaskFileService taskFileService;
    @Mock private TimeSessionRepository timeSessionRepository;

    @InjectMocks
    private TaskService taskService;

    private User employee;
    private User manager;
    private Task selfReportedTask;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(10L);
        employee.setFirstName("Ahmed");
        employee.setLastName("Hassan");
        employee.setEmail("ahmed@example.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setStatus(UserStatus.ACTIVE);

        manager = new User();
        manager.setId(20L);
        manager.setFirstName("Mona");
        manager.setLastName("Ali");
        manager.setEmail("mona@example.com");
        manager.setRole(Role.MANAGER);
        manager.setStatus(UserStatus.ACTIVE);

        selfReportedTask = new Task();
        selfReportedTask.setId(100L);
        selfReportedTask.setTitle("Fix UI Alignment");
        selfReportedTask.setDeadline(LocalDate.now().plusDays(3));
        selfReportedTask.setStatus(TaskStatus.TODO);
        selfReportedTask.setCreatedBy(employee);
        selfReportedTask.setSelfReported(true);
    }

    @Test
    @DisplayName("Employee creates self-reported task: forces Lead & only member, sets self_reported = true, notifies managers")
    void employeeCreatesSelfReportedTask_success() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Fix UI Alignment");
        req.setDeadline(LocalDate.now().plusDays(2));
        req.setPriority(TaskPriority.HIGH);
        // Attempt to pass another user as member or lead
        req.setMemberUserIds(List.of(99L, 100L));
        req.setLeadUserId(99L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(taskRepository.countActiveSelfReportedTasksForUser(10L)).thenReturn(5L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));

        TaskResponse res = taskService.createTask(10L, req);

        // Verify task properties
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();
        assertThat(savedTask.isSelfReported()).isTrue();
        assertThat(savedTask.getCreatedBy().getId()).isEqualTo(10L);

        // Verify only creator was saved as member with role LEAD
        ArgumentCaptor<TaskMember> memberCaptor = ArgumentCaptor.forClass(TaskMember.class);
        verify(taskMemberRepository).save(memberCaptor.capture());
        TaskMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getUser().getId()).isEqualTo(10L);
        assertThat(savedMember.getRole()).isEqualTo(TaskMemberRole.LEAD);

        // Verify notification to managers
        verify(notifications).selfReportedCreated(eq(100L), eq(10L), eq("Ahmed Hassan"), eq("Fix UI Alignment"));

        // Verify history event
        verify(historyService).record(eq(100L), eq(10L), eq("TASK_CREATED"), eq("Self-reported task created"), isNull(), eq("Fix UI Alignment"));
    }

    @Test
    @DisplayName("Employee creation requires deadline; missing deadline throws 400 DEADLINE_REQUIRED")
    void employeeCreatesSelfReportedTask_missingDeadline_throws() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("No Deadline Task");
        req.setDeadline(null);

        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(taskRepository.countActiveSelfReportedTasksForUser(10L)).thenReturn(0L);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Task deadline is required for self-reported tasks");
    }

    @Test
    @DisplayName("Employee creation enforces limit of 20 active self-reported tasks; 21st rejected with 400")
    void employeeCreatesSelfReportedTask_limit20Reached_throws() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("21st Task");
        req.setDeadline(LocalDate.now().plusDays(1));

        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(taskRepository.countActiveSelfReportedTasksForUser(10L)).thenReturn(20L);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("You cannot have more than 20 active self-reported tasks");
    }

    @Test
    @DisplayName("Self-reported task: creator cannot mark Done (403), Manager can")
    void selfReportedTask_donePermissions() {
        selfReportedTask.setStatus(TaskStatus.REVIEW);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));

        // Creator attempts DONE -> 403
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Denied"))
                .when(taskAccessService).requireDonePermission(selfReportedTask, 10L);

        ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
        req.setStatus(TaskStatus.DONE);

        assertThatThrownBy(() -> taskService.changeStatus(100L, 10L, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        // Manager marks DONE -> Allowed
        reset(taskAccessService);
        doNothing().when(taskAccessService).requireDonePermission(selfReportedTask, 20L);
        when(taskRepository.save(any(Task.class))).thenReturn(selfReportedTask);

        taskService.changeStatus(100L, 20L, req);
        verify(taskRepository).save(selfReportedTask);
        assertThat(selfReportedTask.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(notifications).taskDone(100L, 20L, "Fix UI Alignment");
        verify(historyService).record(eq(100L), eq(20L), eq("STATUS_CHANGE"), anyString(), eq("REVIEW"), eq("DONE"));
    }

    @Test
    @DisplayName("Self-reported task: creator cannot send back (403), Manager can")
    void selfReportedTask_sendBackPermissions() {
        selfReportedTask.setStatus(TaskStatus.REVIEW);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));

        SendBackTaskRequest req = new SendBackTaskRequest();
        req.setReason("Please revise changes");

        // Creator attempts send back -> 403
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Denied"))
                .when(taskAccessService).requireSendBackPermission(selfReportedTask, 10L);

        assertThatThrownBy(() -> taskService.sendBack(100L, 10L, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        // Manager sends back -> Allowed
        reset(taskAccessService);
        doNothing().when(taskAccessService).requireSendBackPermission(selfReportedTask, 20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(manager));
        when(subtaskRepository.findByTaskId(100L)).thenReturn(List.of());
        when(checkpointRepository.findByTaskIdAndSubtaskIdIsNullOrderByPositionAscIdAsc(100L)).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenReturn(selfReportedTask);

        taskService.sendBack(100L, 20L, req);
        assertThat(selfReportedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(notifications).taskSentBack(100L, 20L, "Fix UI Alignment", "Please revise changes");
        verify(historyService).record(eq(100L), eq(20L), eq("STATUS_CHANGE"), contains("Reason: Please revise changes"), eq("REVIEW"), eq("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Self-reported task: creator cannot restore (403), Manager can")
    void selfReportedTask_restorePermissions() {
        selfReportedTask.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));

        // Creator attempts restore -> 403
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Denied"))
                .when(taskAccessService).requireRestorePermission(selfReportedTask, 10L);

        assertThatThrownBy(() -> taskService.restoreTask(100L, 10L, TaskStatus.IN_PROGRESS))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        // Manager restores -> Allowed
        reset(taskAccessService);
        doNothing().when(taskAccessService).requireRestorePermission(selfReportedTask, 20L);
        when(taskRepository.save(any(Task.class))).thenReturn(selfReportedTask);

        taskService.restoreTask(100L, 20L, TaskStatus.IN_PROGRESS);
        assertThat(selfReportedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(historyService).record(eq(100L), eq(20L), eq("STATUS_CHANGE"), contains("restored to IN_PROGRESS"), eq("DONE"), eq("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Creator delete rules: creator can delete while TODO with 0 time sessions, forbidden once IN_PROGRESS or sessions exist")
    void selfReportedTask_deleteRules() {
        selfReportedTask.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));

        // 1. Creator deletes in TODO with no sessions -> success
        when(timeSessionRepository.existsByTaskId(100L)).thenReturn(false);
        doNothing().when(taskAccessService).requireDeletePermission(selfReportedTask, 10L, false);

        taskService.deleteTask(100L, 10L);
        verify(historyService).recordTaskDeleted(selfReportedTask, 10L);
        verify(taskRepository).delete(selfReportedTask);

        // 2. Creator attempts delete with existing time session -> 403
        when(timeSessionRepository.existsByTaskId(100L)).thenReturn(true);
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Denied"))
                .when(taskAccessService).requireDeletePermission(selfReportedTask, 10L, true);

        assertThatThrownBy(() -> taskService.deleteTask(100L, 10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        // 3. Creator attempts delete in IN_PROGRESS -> 403
        selfReportedTask.setStatus(TaskStatus.IN_PROGRESS);
        when(timeSessionRepository.existsByTaskId(100L)).thenReturn(false);
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "Denied"))
                .when(taskAccessService).requireDeletePermission(selfReportedTask, 10L, false);

        assertThatThrownBy(() -> taskService.deleteTask(100L, 10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Send to review notifies Managers for self-reported tasks")
    void sendToReview_notifiesManagers() {
        selfReportedTask.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(selfReportedTask));
        when(subtaskRepository.findByTaskId(100L)).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenReturn(selfReportedTask);

        taskService.sendToReview(100L, 10L);

        verify(notifications).selfReportedReview(100L, 10L, "Fix UI Alignment");
        verify(historyService).record(100L, 10L, "STATUS_CHANGE", "Task sent to review", "IN_PROGRESS", "REVIEW");
    }

    @Test
    @DisplayName("TaskAccessService permissions check helper methods")
    void taskAccessService_permissionChecks() {
        TaskAccessService accessService = new TaskAccessService(userRepository, taskMemberRepository, subtaskAssigneeRepository, subtaskRepository);

        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(20L)).thenReturn(Optional.of(manager));

        // Self-reported: creator employee cannot mark Done, Manager can
        assertThat(accessService.canMarkDone(selfReportedTask, 10L)).isFalse();
        assertThat(accessService.canMarkDone(selfReportedTask, 20L)).isTrue();

        // Self-reported: creator employee cannot send back, Manager can
        assertThat(accessService.canSendBack(selfReportedTask, 10L)).isFalse();
        assertThat(accessService.canSendBack(selfReportedTask, 20L)).isTrue();

        // Self-reported: creator employee cannot restore, Manager can
        assertThat(accessService.canRestore(selfReportedTask, 10L)).isFalse();
        assertThat(accessService.canRestore(selfReportedTask, 20L)).isTrue();

        // Deletion: TODO + 0 sessions -> creator can delete
        selfReportedTask.setStatus(TaskStatus.TODO);
        assertThat(accessService.canDelete(selfReportedTask, 10L, false)).isTrue();
        // Deletion: TODO + sessions -> creator cannot delete
        assertThat(accessService.canDelete(selfReportedTask, 10L, true)).isFalse();
        // Deletion: IN_PROGRESS -> creator cannot delete
        selfReportedTask.setStatus(TaskStatus.IN_PROGRESS);
        assertThat(accessService.canDelete(selfReportedTask, 10L, false)).isFalse();
        // Manager can always delete
        assertThat(accessService.canDelete(selfReportedTask, 20L, true)).isTrue();
    }
}
