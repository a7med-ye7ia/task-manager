package com.taskmanagesys.time.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.common.error.FieldErrorItem;
import com.taskmanagesys.audit.service.AuditLogService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskActivityService;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.time.dto.TimeSessionRequest;
import com.taskmanagesys.time.dto.TimeSessionResponse;
import com.taskmanagesys.time.dto.TimeRangeResponse;
import com.taskmanagesys.time.dto.TimeTaskOptionResponse;
import com.taskmanagesys.time.dto.TimeSubtaskOptionResponse;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.model.TimeSource;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.time.repository.TimeSegmentRepository;
import com.taskmanagesys.time.model.TimeSessionState;
import com.taskmanagesys.time.model.TimeSegment;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class TimeSessionService {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private final TimeSessionRepository sessions;
    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final SubtaskAssigneeRepository assignees;
    private final TaskAccessService access;
    private final UserRepository users;
    private final Clock clock;
    private final AuditLogService audits;
    private final TaskActivityService activity;
    private final TimeSegmentRepository segments;
    @Value("${app.time.shift-hours:8}")
    private int shiftHours = 8;
    @Value("${app.time.overtime-max-hours:2}")
    private int overtimeMaxHours = 2;
    @Value("${app.time.pause-timeout-hours:12}")
    private int pauseTimeoutHours = 12;

    @Autowired
    public TimeSessionService(TimeSessionRepository sessions, TaskRepository tasks, SubtaskRepository subtasks,
            SubtaskAssigneeRepository assignees, TaskAccessService access, UserRepository users, Clock clock,
            AuditLogService audits, TaskActivityService activity, TimeSegmentRepository segments) {
        this.sessions = sessions;
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.assignees = assignees;
        this.access = access;
        this.users = users;
        this.clock = clock;
        this.audits = audits;
        this.activity = activity;
        this.segments = segments;
    }

    public TimeSessionService(TimeSessionRepository sessions, TaskRepository tasks, SubtaskRepository subtasks,
            SubtaskAssigneeRepository assignees, TaskAccessService access, UserRepository users, Clock clock,
            AuditLogService audits, TaskActivityService activity) {
        this(sessions, tasks, subtasks, assignees, access, users, clock, audits, activity, null);
    }

    @Transactional
    public TimeSessionResponse start(Long userId, Long taskId, Long subtaskId) {
        return start(userId, taskId, subtaskId, false, false, null);
    }

    @Transactional
    public TimeSessionResponse start(Long userId, Long taskId, Long subtaskId, boolean confirmSwitch,
            boolean confirmOvertime) {
        return start(userId, taskId, subtaskId, confirmSwitch, confirmOvertime, null);
    }

    @Transactional
    public TimeSessionResponse start(Long userId, Long taskId, Long subtaskId, boolean confirmSwitch,
            boolean confirmOvertime, String startNote) {
        Task task = task(taskId);
        canTrack(userId, task, subtaskId);
        if (task.getStatus().name().equals("DONE"))
            throw error(HttpStatus.CONFLICT, "TASK_DONE", "Done tasks cannot track time");
        Instant now = Instant.now(clock);
        if (task.getStatus() == TaskStatus.TODO) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            tasks.save(task);
            activity.logActivity(taskId, userId, "TASK_STATUS_CHANGED",
                    java.util.Map.of("old_status", "TODO", "new_status", "IN_PROGRESS", "automatic", true));
        }
        var previous = sessions.findFirstByUserIdAndEndedAtIsNull(userId);
        if (previous.isPresent() && !previous.get().getTaskId().equals(taskId) && !confirmSwitch)
            throw error(HttpStatus.CONFLICT, "SWITCH_CONFIRMATION_REQUIRED",
                    "Another work session is open. Confirm switching tasks to stop it and save its time.");
        previous.ifPresent(open -> close(open, now, "REPLACED"));
        if (dailyWorked(userId, now) >= (shiftHours + overtimeMaxHours) * 3600)
            throw error(HttpStatus.CONFLICT, "OVERTIME_LIMIT", "The 10 hour daily limit has been reached");
        if (dailyWorked(userId, now) >= shiftHours * 3600 && !confirmOvertime)
            throw error(HttpStatus.CONFLICT, "OVERTIME_CONFIRMATION_REQUIRED",
                    "Your 8 hour shift is complete. Confirm overtime to continue.");
        TimeSession s = new TimeSession();
        s.setUserId(userId);
        s.setTaskId(taskId);
        s.setSubtaskId(subtaskId);
        s.setTaskTitle(task.getTitle());
        s.setStartedAt(now);
        s.setActiveStartedAt(now);
        s.setLastHeartbeatAt(now);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setSource(TimeSource.TIMER);
        s.setState(TimeSessionState.RUNNING);
        s.setOvertimeConfirmed(confirmOvertime);
        LocalDate cairoDay = now.atZone(CAIRO).toLocalDate();
        boolean restarted = sessions.findFirstByUserIdAndTaskIdAndEndedAtIsNotNullAndStartedAtBetweenOrderByStartedAtDesc(
                userId, taskId, cairoDay.atStartOfDay(CAIRO).toInstant(), cairoDay.plusDays(1).atStartOfDay(CAIRO).toInstant()).isPresent();
        if (restarted) {
            if (startNote == null || startNote.trim().length() < 3 || startNote.trim().length() > 200)
                throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A 3 to 200 character note is required when restarting", "startNote", "Why are you starting again?");
            s.setStartNote(startNote.trim());
        }
        return response(sessions.save(s), now);
    }

    @Transactional
    public TimeSessionResponse pause(Long userId, Long id) {
        TimeSession s = owned(id, userId);
        if (s.getState() != TimeSessionState.RUNNING)
            return response(s, Instant.now(clock));
        Instant now = Instant.now(clock);
        accrue(s, now);
        s.setState(TimeSessionState.PAUSED);
        s.setLastHeartbeatAt(now);
        s.setUpdatedAt(now);
        return response(sessions.save(s), now);
    }

    @Transactional
    public TimeSessionResponse resume(Long userId, Long id) {
        TimeSession s = owned(id, userId);
        if (s.getState() == TimeSessionState.ENDED)
            throw error(HttpStatus.CONFLICT, "SESSION_ENDED", "This time session has ended");
        Instant now = Instant.now(clock);
        if (dailyWorked(userId, now) >= (shiftHours + overtimeMaxHours) * 3600)
            throw error(HttpStatus.CONFLICT, "OVERTIME_LIMIT", "The 10 hour daily limit has been reached");
        s.setState(TimeSessionState.RUNNING);
        s.setActiveStartedAt(now);
        s.setLastHeartbeatAt(now);
        s.setUpdatedAt(now);
        return response(sessions.save(s), now);
    }

    @Transactional
    public TimeSessionResponse stop(Long userId, Long id) {
        TimeSession s = owned(id, userId);
        Instant now = Instant.now(clock);
        if (s.getState() == TimeSessionState.RUNNING)
            accrue(s, now);
        s.setState(TimeSessionState.ENDED);
        s.setEndedAt(now);
        s.setEndReason("STOPPED");
        s.setUpdatedAt(now);
        return response(sessions.save(s), now);
    }

    @Transactional
    public TimeSessionResponse heartbeat(Long userId, Long id) {
        TimeSession s = owned(id, userId);
        if (s.getState() != TimeSessionState.RUNNING)
            return response(s, Instant.now(clock));
        Instant now = Instant.now(clock);
        enforceShiftLimit(s, now);
        s.setLastHeartbeatAt(now);
        s.setUpdatedAt(now);
        return response(sessions.save(s), now);
    }

    @Transactional
    public TimeSessionResponse manual(Long userId, TimeSessionRequest req) {
        Task task = task(req.getTaskId());
        canTrack(userId, task, req.getSubtaskId());
        Instant now = Instant.now(clock), start = req.getStartedAt(), end = req.getEndedAt();
        if (start == null)
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Start time is required", "startedAt",
                    "Start time is required");
        if (end == null)
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "End time is required", "endedAt",
                    "End time is required");
        if (!end.isAfter(start))
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "End must be after start", "endedAt",
                    "End must be after start");
        if (end.isAfter(now))
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Time entries cannot be in the future",
                    "endedAt", "End time cannot be in the future");
        if (!isManager(userId) && start.isBefore(now.minus(Duration.ofDays(7))))
            throw fieldError(HttpStatus.FORBIDDEN, "TIME_WINDOW", "Manual entries are limited to the last 7 days",
                    "startedAt", "Employees can enter only the last 7 days");
        if (req.getNote() == null || req.getNote().trim().length() < 3 || req.getNote().trim().length() > 200)
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A note from 3 to 200 characters is required",
                    "note", "Use 3 to 200 characters");
        validateOverlap(userId, start, end, null);
        enforceDailyCap(userId, start, end);
        TimeSession s = new TimeSession();
        s.setUserId(userId);
        s.setTaskId(task.getId());
        s.setSubtaskId(req.getSubtaskId());
        s.setTaskTitle(task.getTitle());
        s.setStartedAt(start);
        s.setEndedAt(end);
        s.setLastHeartbeatAt(end);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setSource(TimeSource.MANUAL);
        s.setState(TimeSessionState.ENDED);
        s.setWorkedSeconds(Duration.between(start, end).getSeconds());
        s.setEndReason("MANUAL");
        s.setNote(req.getNote().trim());
        s.setLastEditedBy(userId);
        TimeSession saved = sessions.save(s);
        audits.record(userId, null, "TIME_ENTRY_ADDED",
                java.util.Map.of("sessionId", saved.getId(), "taskId", task.getId()));
        return response(saved, now);
    }

    @Transactional
    public TimeSessionResponse editManual(Long userId, Long id, TimeSessionRequest req) {
        TimeSession existing = owned(id, userId);
        if (existing.getSource() != TimeSource.MANUAL)
            throw error(HttpStatus.FORBIDDEN, "TIME_ACCESS_DENIED", "Only manual entries can be edited");
        if (!isManager(userId) && existing.getStartedAt().isBefore(Instant.now(clock).minus(Duration.ofDays(7))))
            throw error(HttpStatus.FORBIDDEN, "TIME_WINDOW", "Only a Manager can edit older entries");
        sessions.delete(existing);
        TimeSessionResponse result = manual(userId, req);
        audits.record(userId, null, "TIME_ENTRY_EDITED", java.util.Map.of("sessionId", id, "taskId", req.getTaskId()));
        return result;
    }

    @Transactional
    public void deleteManual(Long userId, Long id) {
        TimeSession s = owned(id, userId);
        if (s.getSource() != TimeSource.MANUAL)
            throw error(HttpStatus.FORBIDDEN, "TIME_ACCESS_DENIED", "Only manual entries can be deleted");
        if (!isManager(userId) && s.getStartedAt().isBefore(Instant.now(clock).minus(Duration.ofDays(7))))
            throw error(HttpStatus.FORBIDDEN, "TIME_WINDOW", "Only a Manager can delete older entries");
        sessions.delete(s);
        audits.record(userId, null, "TIME_ENTRY_DELETED", java.util.Map.of("sessionId", id, "taskId", s.getTaskId()));
    }

    @Transactional(readOnly = true)
    public TimeRangeResponse list(Long userId, LocalDate from, LocalDate to) {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock.withZone(CAIRO));
        LocalDate rangeFrom = from == null ? today.minusDays(6) : from;
        LocalDate rangeTo = to == null ? today : to;
        if (rangeTo.isBefore(rangeFrom))
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The end date cannot be before the start date", "to", "Choose a date on or after the start date");
        if (Duration.between(rangeFrom.atStartOfDay(CAIRO).toInstant(), rangeTo.plusDays(1).atStartOfDay(CAIRO).toInstant()).toDays() > 366)
            throw fieldError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The range can be at most 366 days", "to", "Choose a range of 366 days or less");
        Instant start = rangeFrom.atStartOfDay(CAIRO).toInstant();
        Instant end = rangeTo.plusDays(1).atStartOfDay(CAIRO).toInstant();
        List<TimeSession> rows = sessions.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> s.getStartedAt().isBefore(end) && (s.getEndedAt() == null || s.getEndedAt().isAfter(start))).toList();
        Map<Long, List<TimeSegment>> segmentMap = segments == null ? Map.of() : segments.findBySessionIdInOrderByStartedAtAsc(rows.stream().map(TimeSession::getId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.groupingBy(TimeSegment::getSessionId));
        Map<LocalDate, List<TimeRangeResponse.Entry>> entriesByDay = new TreeMap<>(Comparator.reverseOrder());
        for (TimeSession row : rows) {
            Map<LocalDate, Long> secondsByDay = workedByDay(row, segmentMap.getOrDefault(row.getId(), List.of()), rangeFrom, rangeTo, now);
            for (Map.Entry<LocalDate, Long> entry : secondsByDay.entrySet()) {
                if (entry.getValue() == 0) continue;
                boolean editable = row.getSource() == TimeSource.MANUAL && (isManager(userId) || !row.getStartedAt().isBefore(now.minus(Duration.ofDays(7))));
                entriesByDay.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(new TimeRangeResponse.Entry(row.getId(), row.getTaskId(), row.getSubtaskId(), row.getTaskTitle(), row.getStartedAt(), row.getEndedAt(), entry.getValue(), row.getSource().name(), row.getNote(), row.getStartNote(), row.getState().name(), editable));
            }
        }
        List<TimeRangeResponse.Day> days = entriesByDay.entrySet().stream().map(entry -> {
            long worked = entry.getValue().stream().mapToLong(TimeRangeResponse.Entry::workedSeconds).sum();
            long regular = Math.min(worked, shiftHours * 3600L);
            return new TimeRangeResponse.Day(entry.getKey(), worked, regular, Math.max(0, worked - regular), entry.getValue());
        }).toList();
        long worked = days.stream().mapToLong(TimeRangeResponse.Day::workedSeconds).sum();
        long regular = days.stream().mapToLong(TimeRangeResponse.Day::regularSeconds).sum();
        return new TimeRangeResponse(rangeFrom, rangeTo, worked, regular, worked - regular, days.size(), days);
    }

    private Map<LocalDate, Long> workedByDay(TimeSession session, List<TimeSegment> sessionSegments, LocalDate from, LocalDate to, Instant now) {
        List<TimeSegment> effective = sessionSegments;
        if (effective.isEmpty()) {
            Instant finish = session.getEndedAt() == null ? now : session.getEndedAt();
            if (session.getState() == TimeSessionState.RUNNING && session.getActiveStartedAt() != null) {
                TimeSegment active = new TimeSegment(); active.setStartedAt(session.getActiveStartedAt()); active.setEndedAt(now); effective = List.of(active);
            } else if (session.getSource() == TimeSource.MANUAL || session.getWorkedSeconds() == 0) {
                TimeSegment single = new TimeSegment(); single.setStartedAt(session.getStartedAt()); single.setEndedAt(finish); effective = List.of(single);
            }
        }
        Map<LocalDate, Long> result = new HashMap<>();
        for (TimeSegment segment : effective) {
            LocalDate day = segment.getStartedAt().atZone(CAIRO).toLocalDate();
            LocalDate last = segment.getEndedAt().atZone(CAIRO).toLocalDate();
            while (!day.isAfter(last)) {
                if (!day.isBefore(from) && !day.isAfter(to)) {
                    long overlap = overlap(segment.getStartedAt(), segment.getEndedAt(), day.atStartOfDay(CAIRO).toInstant(), day.plusDays(1).atStartOfDay(CAIRO).toInstant());
                    if (overlap > 0) result.merge(day, overlap, Long::sum);
                }
                day = day.plusDays(1);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Page<TimeTaskOptionResponse> taskOptions(Long userId, String query, Pageable pageable) {
        return tasks.findTrackableTasks(userId, query == null ? "" : query.trim(), pageable)
                .map(TimeTaskOptionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TimeSubtaskOptionResponse> subtaskOptions(Long userId, Long taskId) {
        Task task = task(taskId);
        boolean manager = isManager(userId);
        boolean member = access.member(taskId, userId);
        List<com.taskmanagesys.task.model.Subtask> taskSubtasks = subtasks.findByTaskIdOrderByCreatedAtAsc(taskId);
        if (!manager && !member
                && taskSubtasks.stream().noneMatch(s -> assignees.existsBySubtaskIdAndUserId(s.getId(), userId)))
            throw new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED",
                    "You are not allowed to view these subtasks");
        return taskSubtasks.stream().filter(s -> manager || assignees.existsBySubtaskIdAndUserId(s.getId(), userId))
                .map(TimeSubtaskOptionResponse::from).toList();
    }

    @Transactional
    public int recoverStale(Instant now) {
        int count = 0;
        for (TimeSession s : sessions.findByEndedAtIsNullAndLastHeartbeatAtBefore(now.minus(Duration.ofMinutes(3)))) {
            if (s.getState() == TimeSessionState.RUNNING) {
                pauseAt(s, s.getLastHeartbeatAt(), "HEARTBEAT_TIMEOUT");
                count++;
            }
        }
        for (TimeSession s : sessions
                .findByEndedAtIsNullAndStartedAtBefore(now.minus(Duration.ofHours(pauseTimeoutHours)))) {
            if (s.getState() == TimeSessionState.PAUSED) {
                endAt(s, now, "PAUSE_TIMEOUT");
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int enforceOpenLimits(Instant now) {
        int count = 0;
        for (TimeSession s : sessions.findByEndedAtIsNullAndLastHeartbeatAtBefore(now.plusSeconds(1))) {
            if (s.getState() == TimeSessionState.RUNNING
                    && dailyWorked(s.getUserId(), now) >= (shiftHours + overtimeMaxHours) * 3600) {
                endAt(s, now, "OVERTIME_LIMIT");
                count++;
            }
        }
        return count;
    }

    @Transactional
    public long cleanup(Instant cutoff) {
        return sessions.deleteByStartedAtBefore(cutoff);
    }

    private void close(TimeSession s, Instant end, String reason) {
        if (s.getState() == TimeSessionState.RUNNING)
            accrue(s, end);
        s.setState(TimeSessionState.ENDED);
        s.setEndedAt(end);
        s.setEndReason(reason);
        s.setUpdatedAt(end);
        sessions.save(s);
    }

    private void pauseAt(TimeSession s, Instant at, String reason) {
        if (s.getState() == TimeSessionState.RUNNING)
            accrue(s, at);
        s.setState(TimeSessionState.PAUSED);
        s.setEndReason(reason);
        s.setUpdatedAt(at);
        sessions.save(s);
    }

    private void endAt(TimeSession s, Instant at, String reason) {
        if (s.getState() == TimeSessionState.RUNNING)
            accrue(s, at);
        s.setState(TimeSessionState.ENDED);
        s.setEndedAt(at);
        s.setEndReason(reason);
        s.setUpdatedAt(at);
        sessions.save(s);
    }

    private void accrue(TimeSession s, Instant at) {
        if (s.getActiveStartedAt() == null)
            return;
        long seconds = Math.max(0, at.getEpochSecond() - s.getActiveStartedAt().getEpochSecond());
        s.setWorkedSeconds(s.getWorkedSeconds() + seconds);
        if (segments != null && s.getId() != null) {
            TimeSegment segment = new TimeSegment();
            segment.setSessionId(s.getId());
            segment.setStartedAt(s.getActiveStartedAt());
            segment.setEndedAt(at);
            segment.setWorkedSeconds(seconds);
            segments.save(segment);
        }
        s.setActiveStartedAt(null);
    }

    private TimeSession owned(Long id, Long userId) {
        TimeSession s = sessions.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Time session not found"));
        if (!s.getUserId().equals(userId))
            throw error(HttpStatus.FORBIDDEN, "TIME_ACCESS_DENIED", "You cannot change this time session");
        return s;
    }

    private Task task(Long id) {
        return tasks.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found"));
    }

    private void canTrack(Long userId, Task task, Long subtaskId) {
        if (subtaskId == null) {
            if (!access.manager(userId) && !access.member(task.getId(), userId))
                throw fieldError(HttpStatus.FORBIDDEN, "TIME_ACCESS_DENIED",
                        "You must be a member of this task to track time", "taskId", "Choose a task you work on");
            return;
        }
        if (subtasks.findById(subtaskId).filter(s -> s.getTask().getId().equals(task.getId())).isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "SUBTASK_NOT_FOUND", "Subtask not found");
        try {
            access.requireSubtaskAssigneeOrLead(task.getId(), subtaskId, userId);
        } catch (AppException ex) {
            throw fieldError(HttpStatus.FORBIDDEN, "TIME_ACCESS_DENIED", "You are not assigned to this subtask",
                    "subtaskId", "Choose one of your subtasks");
        }
    }

    private boolean isManager(Long userId) {
        return users.findById(userId).map(u -> u.getRole() == Role.MANAGER).orElse(false);
    }

    private void enforceMaximum(TimeSession s, Instant now) {
        enforceShiftLimit(s, now);
    }

    private void enforceShiftLimit(TimeSession s, Instant now) {
        if (dailyWorked(s.getUserId(), now) >= (shiftHours + overtimeMaxHours) * 3600) {
            endAt(s, now, "OVERTIME_LIMIT");
            throw error(HttpStatus.CONFLICT, "OVERTIME_LIMIT", "The 10 hour daily limit has been reached");
        }
    }

    private long dailyWorked(Long userId, Instant now) {
        LocalDate day = now.atZone(CAIRO).toLocalDate();
        Instant from = day.atStartOfDay(CAIRO).toInstant(), to = day.plusDays(1).atStartOfDay(CAIRO).toInstant();
        long total = 0;
        for (TimeSession s : sessions.findByUserIdOrderByStartedAtDesc(userId)) {
            if (segments != null && s.getId() != null) {
                for (TimeSegment segment : segments.findBySessionIdOrderByStartedAtAsc(s.getId()))
                    total += overlap(segment.getStartedAt(), segment.getEndedAt(), from, to);
            } else {
                Instant end = s.getEndedAt() == null ? now : s.getEndedAt();
                total += overlap(s.getStartedAt(), end, from, to);
            }
            if (s.getState() == TimeSessionState.RUNNING && s.getActiveStartedAt() != null)
                total += overlap(s.getActiveStartedAt(), now, from, to);
        }
        return total;
    }

    private void validateOverlap(Long userId, Instant start, Instant end, Long ignore) {
        for (TimeSession s : sessions.findByUserIdOrderByStartedAtDesc(userId)) {
            if (ignore != null && ignore.equals(s.getId()))
                continue;
            Instant otherEnd = s.getEndedAt() == null ? Instant.now(clock) : s.getEndedAt();
            if (start.isBefore(otherEnd) && end.isAfter(s.getStartedAt()))
                throw error(HttpStatus.CONFLICT, "TIME_OVERLAP", "Time entries cannot overlap");
        }
    }

    private void enforceDailyCap(Long userId, Instant start, Instant end) {
        ZoneId z = CAIRO;
        LocalDate day = start.atZone(z).toLocalDate();
        while (!day.isAfter(end.atZone(z).toLocalDate())) {
            Instant ds = day.atStartOfDay(z).toInstant(), de = day.plusDays(1).atStartOfDay(z).toInstant();
            long seconds = 0;
            for (TimeSession s : sessions.findByUserIdOrderByStartedAtDesc(userId)) {
                Instant se = s.getEndedAt() == null ? Instant.now(clock) : s.getEndedAt();
                seconds += overlap(s.getStartedAt(), se, ds, de);
            }
            seconds += overlap(start, end, ds, de);
            if (seconds > (shiftHours + overtimeMaxHours) * 3600)
                throw error(HttpStatus.CONFLICT, "DAILY_CAP", "The daily 10 hour limit would be exceeded");
            day = day.plusDays(1);
        }
    }

    private long overlap(Instant a, Instant b, Instant c, Instant d) {
        Instant from = a.isAfter(c) ? a : c, to = b.isBefore(d) ? b : d;
        return Math.max(0, to.getEpochSecond() - from.getEpochSecond());
    }

    private TimeSessionResponse response(TimeSession s, Instant now) {
        return TimeSessionResponse.from(s, now);
    }

    private AppException error(HttpStatus status, String code, String message) {
        return new AppException(status, code, message);
    }

    private AppException fieldError(HttpStatus status, String code, String message, String field, String detail) {
        return new AppException(status, code, message, List.of(new FieldErrorItem(field, detail)));
    }
}
