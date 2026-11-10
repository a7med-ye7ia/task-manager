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
import com.taskmanagesys.history.service.HistoryService;
import com.taskmanagesys.time.service.TimeSessionService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TimeSessionService timeSessions; private final HistoryService history;
    private final TaskFileService taskFiles;
    @Autowired
    public ArchiveHousekeepingJob(TaskRepository tasks, TaskMemberRepository members, NotificationService notifications, AuditLogRepository auditLogs, EmailTokenRepository emailTokens, RefreshTokenRepository refreshTokens, EmailOutboxRepository outbox, Clock clock, TimeSessionService timeSessions, HistoryService history, TaskFileService taskFiles) {
        this.tasks=tasks; this.members=members; this.notifications=notifications; this.auditLogs=auditLogs; this.emailTokens=emailTokens; this.refreshTokens=refreshTokens; this.outbox=outbox; this.clock=clock; this.timeSessions=timeSessions; this.history=history; this.taskFiles=taskFiles;
    }
    public ArchiveHousekeepingJob(TaskRepository tasks, TaskMemberRepository members, NotificationService notifications, AuditLogRepository auditLogs, EmailTokenRepository emailTokens, RefreshTokenRepository refreshTokens, EmailOutboxRepository outbox, Clock clock) {
        this(tasks,members,notifications,auditLogs,emailTokens,refreshTokens,outbox,clock,null,null,null);
    }
    public ArchiveHousekeepingJob(TaskRepository tasks, TaskMemberRepository members, NotificationService notifications, AuditLogRepository auditLogs, EmailTokenRepository emailTokens, RefreshTokenRepository refreshTokens, EmailOutboxRepository outbox, Clock clock, TimeSessionService timeSessions, HistoryService history) {
        this(tasks,members,notifications,auditLogs,emailTokens,refreshTokens,outbox,clock,timeSessions,history,null);
    }
    @Scheduled(cron = "${app.archive.cron:0 0 2 * * *}", zone = "${app.archive.zone:Africa/Cairo}")
    public void run() { Instant now = Instant.now(clock); if (timeSessions != null) timeSessions.recoverStale(now); warn(now); purge(now); cleanup(now); if (taskFiles != null) log.info("Orphan task files removed={}", taskFiles.cleanupOrphans()); }
    @Transactional
    void warn(Instant now) {
        List<Task> rows = tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtBetween(TaskStatus.DONE, now, now.plus(Duration.ofDays(3)));
        int count=0; for (Task task : rows) if (task.getPurgeWarningSentAt() == null) { var lead = members.findByTaskIdAndRole(task.getId(), TaskMemberRole.LEAD); notifications.archiveWarning(task.getId(), null, lead.map(m -> m.getUserId()).orElse(null), task.getTitle()); task.setPurgeWarningSentAt(now); tasks.save(task); count++; }
        log.info("Archive warning step touched {} tasks", count);
    }
    @Transactional
    void purge(Instant now) {
        int count=0; for (Task task : tasks.findByStatusAndPurgeAtIsNotNullAndPurgeAtLessThanEqual(TaskStatus.DONE, now)) { try { if (history != null) history.recordTaskDeleted(task, null); if (taskFiles != null) taskFiles.deleteForTask(task.getId()); tasks.delete(task); count++; } catch (RuntimeException ex) { log.warn("Could not purge task {}", task.getId(), ex); } }
        log.info("Archive purge step touched {} tasks", count);
    }
    @Transactional
    void cleanup(Instant now) {
        long audit = auditLogs.deleteByCreatedAtBefore(now.minus(Duration.ofDays(183)));
        Instant tokenCutoff = now.minus(Duration.ofDays(7));
        long tokens = emailTokens.deleteByUsedAtIsNotNullAndUsedAtBefore(tokenCutoff) + emailTokens.deleteByExpiresAtBefore(tokenCutoff);
        long refresh = refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(tokenCutoff, tokenCutoff);
        long sent = outbox.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, now.minus(Duration.ofDays(30)));
        long sessions = timeSessions == null ? 0 : timeSessions.cleanup(now.minus(Duration.ofDays(730)));
        long historyRows = history == null ? 0 : history.cleanup(now.minus(Duration.ofDays(730)));
        log.info("Housekeeping touched audit={}, emailTokens={}, refreshTokens={}, sentOutbox={}, timeSessions={}, history={}", audit, tokens, refresh, sent, sessions, historyRows);
    }
}
