package com.taskmanagesys.task;

import com.taskmanagesys.audit.repository.AuditLogRepository;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.history.service.HistoryService;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.ArchiveHousekeepingJob;
import com.taskmanagesys.time.service.TimeSessionService;
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
class Phase8HousekeepingTest {
    @Mock TaskRepository tasks; @Mock TaskMemberRepository members; @Mock NotificationService notifications; @Mock AuditLogRepository audits;
    @Mock EmailTokenRepository emailTokens; @Mock RefreshTokenRepository refreshTokens; @Mock EmailOutboxRepository outbox; @Mock TimeSessionService timeSessions; @Mock HistoryService history;

    @Test void purgeRecordsHistoryBeforeDeletingTaskAndCleanupKeepsTimeSessionsIndependent() {
        Instant now=Instant.parse("2026-11-02T00:00:00Z"); Task task=new Task(); task.setId(9L); task.setTitle("Archived"); task.setStatus(TaskStatus.DONE); task.setPurgeAt(now.minusSeconds(1));
        when(tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtLessThanEqual(TaskStatus.DONE, now)).thenReturn(List.of(task));
        when(tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(any(),any(),any())).thenReturn(List.of());
        when(audits.deleteByCreatedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByUsedAtIsNotNullAndUsedAtBefore(any())).thenReturn(0L); when(emailTokens.deleteByExpiresAtBefore(any())).thenReturn(0L); when(refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(any(),any())).thenReturn(0L); when(outbox.deleteByStatusAndCreatedAtBefore(any(),any())).thenReturn(0L); when(timeSessions.cleanup(any())).thenReturn(0L); when(history.cleanup(any())).thenReturn(0L);
        ArchiveHousekeepingJob job=new ArchiveHousekeepingJob(tasks,members,notifications,audits,emailTokens,refreshTokens,outbox,Clock.fixed(now,ZoneOffset.UTC),timeSessions,history);
        job.run();
        verify(history).recordTaskDeleted(task,null); verify(tasks).delete(task); verify(timeSessions).cleanup(any());
    }
}
