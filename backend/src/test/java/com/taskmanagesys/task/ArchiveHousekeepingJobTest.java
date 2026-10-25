package com.taskmanagesys.task;

import com.taskmanagesys.audit.repository.AuditLogRepository;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.ArchiveHousekeepingJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveHousekeepingJobTest {
    @Mock TaskRepository tasks; @Mock TaskMemberRepository members; @Mock NotificationService notifications;
    @Mock AuditLogRepository audits; @Mock EmailTokenRepository emailTokens; @Mock RefreshTokenRepository refreshTokens; @Mock EmailOutboxRepository outbox;

    private ArchiveHousekeepingJob job(Instant now) {
        return new ArchiveHousekeepingJob(tasks, members, notifications, audits, emailTokens, refreshTokens, outbox, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void warningIsSentOnceWithinThreeDays() {
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        Task task = new Task(); task.setId(7L); task.setTitle("Archive me"); task.setStatus(TaskStatus.DONE); task.setPurgeAt(now.plusSeconds(86400));
        when(tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(eq(TaskStatus.DONE), eq(now), eq(now.plusSeconds(259200)))).thenReturn(List.of(task));
        when(members.findByTaskIdAndRole(eq(7L), any())).thenReturn(java.util.Optional.empty());
        when(audits.deleteByCreatedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByUsedAtIsNotNullAndUsedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByExpiresAtBefore(any())).thenReturn(0L); when(refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(any(), any())).thenReturn(0L); when(outbox.deleteByStatusAndCreatedAtBefore(any(), any())).thenReturn(0L);
        job(now).run();
        verify(notifications).archiveWarning(7L, null, null, "Archive me");
        verify(tasks).save(task);
    }

    @Test
    void expiredTasksArePurgedAndOneFailureDoesNotStopTheBatch() {
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        Task first = new Task(); first.setId(1L); first.setStatus(TaskStatus.DONE); first.setPurgeAt(now.minusSeconds(1));
        Task second = new Task(); second.setId(2L); second.setStatus(TaskStatus.DONE); second.setPurgeAt(now.minusSeconds(1));
        when(tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtLessThanEqual(TaskStatus.DONE, now)).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("db")).when(tasks).delete(first);
        when(tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(any(), any(), any())).thenReturn(List.of());
        when(audits.deleteByCreatedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByUsedAtIsNotNullAndUsedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByExpiresAtBefore(any())).thenReturn(0L); when(refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(any(), any())).thenReturn(0L); when(outbox.deleteByStatusAndCreatedAtBefore(any(), any())).thenReturn(0L);
        job(now).run();
        verify(tasks).delete(first); verify(tasks).delete(second);
    }
}
