package com.taskmanagesys.time;

import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskActivityService;
import com.taskmanagesys.time.dto.TimeSessionRequest;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.time.service.TimeSessionService;
import com.taskmanagesys.user.repository.UserRepository;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSessionServiceTest {
    @Mock TimeSessionRepository sessions; @Mock TaskRepository tasks; @Mock SubtaskRepository subtasks;
    @Mock SubtaskAssigneeRepository assignees; @Mock TaskAccessService access; @Mock UserRepository users;
    @Mock AuditLogService audits; @Mock TaskActivityService activity;

    private TimeSessionService service(Instant now) {
        return new TimeSessionService(sessions, tasks, subtasks, assignees, access, users, Clock.fixed(now, ZoneOffset.UTC), audits, activity);
    }
    private Task task(TaskStatus status) { Task task = new Task(); task.setId(7L); task.setTitle("Tracked task"); task.setStatus(status); return task; }

    @Test void doneTasksCannotStart() {
        when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.DONE)));
        assertThrows(AppException.class, () -> service(Instant.parse("2026-11-01T12:00:00Z")).start(3L, 7L, null));
    }

    @Test void startingAnotherTimerReplacesTheFirstAndTodoMovesAutomatically() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z"); Task task = task(TaskStatus.TODO);
        TimeSession previous = new TimeSession(); previous.setUserId(3L); previous.setStartedAt(now.minusSeconds(600)); previous.setLastHeartbeatAt(now.minusSeconds(10));
        when(tasks.findById(7L)).thenReturn(Optional.of(task)); when(sessions.findFirstByUserIdAndEndedAtIsNull(3L)).thenReturn(Optional.of(previous));
        when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of()); when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service(now).start(3L, 7L, null);
        verify(activity).logActivity(eq(7L), eq(3L), eq("TASK_STATUS_CHANGED"), any());
        verify(sessions, atLeast(2)).save(any());
    }

    @Test void manualEntryRejectsFutureAndInvalidNote() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(now.minusSeconds(60)); request.setEndedAt(now.plusSeconds(60)); request.setNote("ok");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
        request.setEndedAt(now.minusSeconds(1)); request.setNote("x");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
    }

    @Test void manualEntriesCannotOverlap() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        TimeSession existing = new TimeSession(); existing.setUserId(3L); existing.setStartedAt(now.minusSeconds(600)); existing.setEndedAt(now.minusSeconds(300));
        when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of(existing));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(now.minusSeconds(500)); request.setEndedAt(now.minusSeconds(400)); request.setNote("valid note");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
    }

    @Test void outsiderCannotTrackTask() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        doThrow(new AppException(org.springframework.http.HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "denied")).when(access).requireMember(7L, 99L);
        assertThrows(AppException.class, () -> service(now).start(99L, 7L, null));
    }

    @Test void outsiderManualEntryReturnsTaskFieldError() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");
        when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        when(access.manager(99L)).thenReturn(false);
        when(access.member(7L, 99L)).thenReturn(false);
        TimeSessionRequest request = new TimeSessionRequest();
        request.setTaskId(7L);
        request.setStartedAt(now.minusSeconds(600));
        request.setEndedAt(now.minusSeconds(300));
        request.setNote("valid note");
        AppException exception = assertThrows(AppException.class, () -> service(now).manual(99L, request));
        org.junit.jupiter.api.Assertions.assertEquals("taskId", exception.getFieldErrors().get(0).getField());
    }

    @Test void dailyCapIncludesSessionsAcrossCairoMidnight() {
        Instant now = Instant.parse("2026-11-02T03:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        TimeSession existing = new TimeSession(); existing.setUserId(3L); existing.setStartedAt(Instant.parse("2026-11-01T20:00:00Z")); existing.setEndedAt(Instant.parse("2026-11-02T02:00:00Z"));
        when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of(existing));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(Instant.parse("2026-11-01T18:00:00Z")); request.setEndedAt(Instant.parse("2026-11-02T02:30:00Z")); request.setNote("cross midnight");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
    }

    @Test void staleAndEightHourSessionsAreClosedByRecovery() {
        Instant now = Instant.parse("2026-11-02T12:00:00Z");
        TimeSession stale = new TimeSession(); stale.setStartedAt(now.minusSeconds(600)); stale.setLastHeartbeatAt(now.minusSeconds(301));
        TimeSession longRunning = new TimeSession(); longRunning.setStartedAt(now.minus(Duration.ofHours(9))); longRunning.setLastHeartbeatAt(now.minusSeconds(30));
        when(sessions.findByEndedAtIsNullAndLastHeartbeatAtBefore(any())).thenReturn(List.of(stale)); when(sessions.findByEndedAtIsNullAndStartedAtBefore(any())).thenReturn(List.of(longRunning));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service(now).recoverStale(now);
        verify(sessions, atLeast(2)).save(any());
    }

    @Test void managerCanEditAnOlderManualEntry() {
        Instant now = Instant.parse("2026-11-02T12:00:00Z"); TimeSession old = new TimeSession(); old.setId(5L); old.setUserId(3L); old.setSource(com.taskmanagesys.time.model.TimeSource.MANUAL); old.setStartedAt(now.minus(Duration.ofDays(8))); old.setEndedAt(now.minus(Duration.ofDays(8)).plusSeconds(600));
        when(sessions.findById(5L)).thenReturn(Optional.of(old)); User manager = new User(); manager.setRole(Role.MANAGER); when(users.findById(3L)).thenReturn(Optional.of(manager)); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS))); when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of()); when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(now.minusSeconds(600)); request.setEndedAt(now.minusSeconds(300)); request.setNote("manager correction");
        service(now).editManual(3L, 5L, request);
        verify(audits).record(eq(3L), isNull(), eq("TIME_ENTRY_EDITED"), any());
    }

    @Test void employeeCannotAddEntryOlderThanSevenDays() {
        Instant now = Instant.parse("2026-11-02T12:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS))); User employee = new User(); employee.setRole(Role.EMPLOYEE); when(users.findById(3L)).thenReturn(Optional.of(employee));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(now.minus(Duration.ofDays(8))); request.setEndedAt(now.minus(Duration.ofDays(8)).plusSeconds(600)); request.setNote("old entry");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
    }

    @Test void dailyCapRejectsTheThirteenthHour() {
        Instant now = Instant.parse("2026-11-02T12:00:00Z"); when(tasks.findById(7L)).thenReturn(Optional.of(task(TaskStatus.IN_PROGRESS)));
        TimeSession existing = new TimeSession(); existing.setUserId(3L); existing.setStartedAt(now.minus(Duration.ofHours(12))); existing.setEndedAt(now);
        when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of(existing));
        TimeSessionRequest request = new TimeSessionRequest(); request.setTaskId(7L); request.setStartedAt(now.minusSeconds(600)); request.setEndedAt(now.minusSeconds(1)); request.setNote("extra hour");
        assertThrows(AppException.class, () -> service(now).manual(3L, request));
    }

    @Test void listBuildsCairoDayTotalsFromOnlyTheCallerSessions() {
        Instant now = Instant.parse("2026-11-05T12:00:00Z");
        TimeSession session = new TimeSession(); session.setId(1L); session.setUserId(3L); session.setTaskId(7L); session.setTaskTitle("Tracked task"); session.setSource(com.taskmanagesys.time.model.TimeSource.MANUAL); session.setState(com.taskmanagesys.time.model.TimeSessionState.ENDED); session.setStartedAt(Instant.parse("2026-11-04T21:00:00Z")); session.setEndedAt(Instant.parse("2026-11-04T23:00:00Z"));
        when(sessions.findByUserIdOrderByStartedAtDesc(3L)).thenReturn(List.of(session));
        var range = service(now).list(3L, java.time.LocalDate.of(2026, 11, 4), java.time.LocalDate.of(2026, 11, 5));
        assertEquals(7200, range.workedSeconds());
        assertEquals(2, range.daysWorked());
        assertEquals(3600, range.days().get(0).workedSeconds());
    }
}
