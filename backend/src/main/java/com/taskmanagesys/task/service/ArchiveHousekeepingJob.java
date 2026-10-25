package com.taskmanagesys.task.service;

import com.taskmanagesys.audit.repository.AuditLogRepository;
import com.taskmanagesys.auth.repository.EmailTokenRepository;
import com.taskmanagesys.auth.repository.RefreshTokenRepository;
import com.taskmanagesys.email.model.EmailStatus;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveHousekeepingJob {
    private static final Logger log = LoggerFactory.getLogger(ArchiveHousekeepingJob.class);
    private final TaskRepository tasks; private final TaskMemberRepository members; private final NotificationService notifications;
    private final AuditLogRepository auditLogs; private final EmailTokenRepository emailTokens; private final RefreshTokenRepository refreshTokens; private final EmailOutboxRepository outbox; private final Clock clock;
    public ArchiveHousekeepingJob(TaskRepository tasks, TaskMemberRepository members, NotificationService notifications, AuditLogRepository auditLogs, EmailTokenRepository emailTokens, RefreshTokenRepository refreshTokens, EmailOutboxRepository outbox, Clock clock) {
        this.tasks=tasks; this.members=members; this.notifications=notifications; this.auditLogs=auditLogs; this.emailTokens=emailTokens; this.refreshTokens=refreshTokens; this.outbox=outbox; this.clock=clock;
    }
    @Scheduled(cron = "${app.archive.cron:0 0 2 * * *}", zone = "${app.archive.zone:Africa/Cairo}")
    public void run() { Instant now = Instant.now(clock); warn(now); purge(now); cleanup(now); }
    @Transactional
    void warn(Instant now) {
        List<Task> rows = tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(TaskStatus.DONE, now, now.plus(Duration.ofDays(3)));
        int count=0; for (Task task : rows) if (task.getPurgeWarningSentAt() == null) { var lead = members.findByTaskIdAndRole(task.getId(), TaskMemberRole.LEAD); notifications.archiveWarning(task.getId(), null, lead.map(m -> m.getUserId()).orElse(null), task.getTitle()); task.setPurgeWarningSentAt(now); tasks.save(task); count++; }
        log.info("Archive warning step touched {} tasks", count);
    }
    @Transactional
    void purge(Instant now) {
        int count=0; for (Task task : tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtLessThanEqual(TaskStatus.DONE, now)) { try { tasks.delete(task); count++; } catch (RuntimeException ex) { log.warn("Could not purge task {}", task.getId(), ex); } }
        log.info("Archive purge step touched {} tasks", count);
    }
    @Transactional
    void cleanup(Instant now) {
        long audit = auditLogs.deleteByCreatedAtBefore(now.minus(Duration.ofDays(183)));
        Instant tokenCutoff = now.minus(Duration.ofDays(7));
        long tokens = emailTokens.deleteByUsedAtIsNotNullAndUsedAtBefore(tokenCutoff) + emailTokens.deleteByExpiresAtBefore(tokenCutoff);
        long refresh = refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(tokenCutoff, tokenCutoff);
        long sent = outbox.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, now.minus(Duration.ofDays(30)));
        log.info("Housekeeping touched audit={}, emailTokens={}, refreshTokens={}, sentOutbox={}", audit, tokens, refresh, sent);
    }
}
