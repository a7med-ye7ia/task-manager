package com.taskmanagesys.time;

import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskActivityService;
import com.taskmanagesys.time.dto.TimeTaskOptionResponse;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.time.service.TimeSessionService;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeTaskOptionsServiceTest {
    @Mock TimeSessionRepository sessions;
    @Mock TaskRepository tasks;
    @Mock SubtaskRepository subtasks;
    @Mock SubtaskAssigneeRepository assignees;
    @Mock TaskAccessService access;
    @Mock UserRepository users;
    @Mock AuditLogService audits;
    @Mock TaskActivityService activity;

    @Test void taskOptionsUseCallerScopedSearchPagingAndMinimalResponse() {
        Task task = new Task();
        task.setId(41L);
        task.setTitle("Quarterly review");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDeadline(LocalDate.of(2026, 11, 20));
        PageRequest page = PageRequest.of(2, 20);
        when(tasks.findTrackableTasks(7L, "review", page)).thenReturn(new PageImpl<>(List.of(task), page, 41));

        TimeSessionService service = new TimeSessionService(sessions, tasks, subtasks, assignees, access, users,
                Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC), audits, activity);
        var result = service.taskOptions(7L, " review ", page);

        assertEquals(1, result.getTotalElements());
        TimeTaskOptionResponse option = result.getContent().get(0);
        assertEquals(41L, option.id());
        assertEquals("Quarterly review", option.title());
        assertEquals(TaskStatus.IN_PROGRESS, option.status());
        assertEquals(LocalDate.of(2026, 11, 20), option.deadline());
        verify(tasks).findTrackableTasks(eq(7L), eq("review"), eq(page));
    }
}
