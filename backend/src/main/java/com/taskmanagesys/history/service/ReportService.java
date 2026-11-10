package com.taskmanagesys.history.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.history.dto.*;
import com.taskmanagesys.history.model.WorkHistory;
import com.taskmanagesys.history.model.WorkHistoryEvent;
import com.taskmanagesys.history.repository.WorkHistoryEventRepository;
import com.taskmanagesys.history.repository.WorkHistoryRepository;
import com.taskmanagesys.time.model.TimeSegment;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.repository.TimeSegmentRepository;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.user.dto.UserSummary;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    public static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final UserRepository userRepository;
    private final WorkHistoryRepository workHistoryRepository;
    private final WorkHistoryEventRepository workHistoryEventRepository;
    private final TimeSessionRepository timeSessionRepository;
    private final TimeSegmentRepository timeSegmentRepository;
    private final Clock clock;

    public ReportService(
            UserRepository userRepository,
            WorkHistoryRepository workHistoryRepository,
            WorkHistoryEventRepository workHistoryEventRepository,
            TimeSessionRepository timeSessionRepository,
            TimeSegmentRepository timeSegmentRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.workHistoryRepository = workHistoryRepository;
        this.workHistoryEventRepository = workHistoryEventRepository;
        this.timeSessionRepository = timeSessionRepository;
        this.timeSegmentRepository = timeSegmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserReportResponse generateReport(Long actorId, Long targetUserId, LocalDate from, LocalDate to) {
        User actor = userRepository.findById(actorId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        User target = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Target user not found"));

        // BR-59: A Manager may open any employee's report; an employee may open only his own.
        if (actor.getRole() != Role.MANAGER && !actor.getId().equals(targetUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to view this report");
        }

        // BR-59: Date range validation
        if (from == null || to == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DATE_REQUIRED", "'from' and 'to' dates are required");
        }
        if (to.isBefore(from)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "'to' date cannot be before 'from' date");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DATE_RANGE_EXCEEDED", "Date range cannot exceed 366 days");
        }

        LocalDate todayInCairo = LocalDate.now(clock.withZone(CAIRO));
        LocalDate retentionLimit = todayInCairo.minusDays(730); // 2-year retention window
        if (from.isBefore(retentionLimit)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DATE_BEFORE_RETENTION", "Date range cannot precede the 2-year retention period");
        }

        Instant rangeStart = from.atStartOfDay(CAIRO).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(CAIRO).toInstant();

        // 1. Batched load of work history
        List<WorkHistory> histories = workHistoryRepository.findByUserId(targetUserId);

        // 2. Batched load of events
        List<Long> historyIds = histories.stream().map(WorkHistory::getId).toList();
        List<WorkHistoryEvent> allEvents = historyIds.isEmpty()
                ? List.of()
                : workHistoryEventRepository.findByHistoryIdInOrderByOccurredAtAsc(historyIds);
        Map<Long, List<WorkHistoryEvent>> eventsByHistoryId = allEvents.stream()
                .collect(Collectors.groupingBy(WorkHistoryEvent::getHistoryId));

        // 3. Batched load of time sessions and segments
        List<TimeSession> sessions = timeSessionRepository.findSessionsForReport(targetUserId, rangeStart, rangeEnd);
        List<Long> sessionIds = sessions.stream().map(TimeSession::getId).toList();
        List<TimeSegment> allSegments = sessionIds.isEmpty()
                ? List.of()
                : timeSegmentRepository.findBySessionIdInOrderByStartedAtAsc(sessionIds);
        Map<Long, List<TimeSegment>> segmentsBySessionId = allSegments.stream()
                .collect(Collectors.groupingBy(TimeSegment::getSessionId));

        // 4. Split hours by Cairo day and calculate worked seconds per task
        Map<LocalDate, Long> dayTotals = new HashMap<>();
        Map<Long, Long> taskWorkedSeconds = new HashMap<>();

        Instant now = Instant.now(clock);
        for (TimeSession s : sessions) {
            List<TimeSegment> segs = segmentsBySessionId.get(s.getId());
            if (segs == null || segs.isEmpty()) {
                if (s.getStartedAt() != null) {
                    Instant end = s.getEndedAt() != null ? s.getEndedAt() : now;
                    accumulateTime(s.getTaskId(), s.getStartedAt(), end, from, to, dayTotals, taskWorkedSeconds);
                }
            } else {
                for (TimeSegment seg : segs) {
                    if (seg.getStartedAt() != null) {
                        Instant end = seg.getEndedAt() != null ? seg.getEndedAt() : now;
                        accumulateTime(s.getTaskId(), seg.getStartedAt(), end, from, to, dayTotals, taskWorkedSeconds);
                    }
                }
            }
        }

        // Build daily breakdown
        List<DailyHoursDto> hoursPerDay = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            long sec = dayTotals.getOrDefault(cur, 0L);
            double hrs = Math.round((sec / 3600.0) * 100.0) / 100.0;
            hoursPerDay.add(new DailyHoursDto(cur, sec, hrs));
            cur = cur.plusDays(1);
        }

        // 5. Build per-task cards from history
        List<ReportTaskCardDto> taskCards = new ArrayList<>();
        for (WorkHistory history : histories) {
            List<WorkHistoryEvent> historyEvents = eventsByHistoryId.getOrDefault(history.getId(), List.of());
            List<ReportEventDto> rangeEvents = historyEvents.stream()
                    .filter(e -> !e.getOccurredAt().isBefore(rangeStart) && e.getOccurredAt().isBefore(rangeEnd))
                    .map(e -> new ReportEventDto(e.getEventType(), e.getSubtaskTitle(), e.getEventCount(), e.getOccurredAt()))
                    .toList();

            long workedSec = taskWorkedSeconds.getOrDefault(history.getTaskId(), 0L);

            // Inclusion check (BR-59):
            // Include tasks where person was a member at any time in range AND task was open at any time in range
            boolean memberInRange = history.getJoinedAt().isBefore(rangeEnd) &&
                    (history.getLeftAt() == null || history.getLeftAt().isAfter(rangeStart));

            boolean openInRange = true;
            if (history.getFinalStatus() != null && ("DONE".equals(history.getFinalStatus()) || "TASK_DELETED".equals(history.getFinalStatus()))) {
                Optional<WorkHistoryEvent> closeEvent = historyEvents.stream()
                        .filter(e -> "TASK_DONE".equals(e.getEventType()) || "TASK_DELETED".equals(e.getEventType()))
                        .max(Comparator.comparing(WorkHistoryEvent::getOccurredAt));
                if (closeEvent.isPresent() && closeEvent.get().getOccurredAt().isBefore(rangeStart)) {
                    openInRange = false;
                }
            }

            if (workedSec > 0 || (memberInRange && openInRange)) {
                ReportTaskCardDto card = new ReportTaskCardDto();
                card.setTaskId(history.getTaskId());
                card.setTitle(history.getTaskTitle());
                card.setPriority(history.getPriority());
                card.setStatus(history.getFinalStatus());
                card.setRole(history.getTaskRole());
                card.setSelfReported(history.isSelfReported());
                card.setDeadline(history.getDeadline());
                card.setWorkedSeconds(workedSec);
                card.setHours(Math.round((workedSec / 3600.0) * 100.0) / 100.0);
                card.setEvents(rangeEvents);

                int subtasksDone = (int) rangeEvents.stream().filter(e -> "SUBTASK_DONE".equals(e.getEventType())).count();
                int checkpointsTicked = rangeEvents.stream().filter(e -> "CHECKPOINT_TICKED".equals(e.getEventType())).mapToInt(e -> e.getCount() != null ? e.getCount() : 1).sum();
                int commentsCount = rangeEvents.stream().filter(e -> "COMMENT_ADDED".equals(e.getEventType())).mapToInt(e -> e.getCount() != null ? e.getCount() : 1).sum();

                card.setSubtasksCompletedCount(subtasksDone);
                card.setCheckpointsTickedCount(checkpointsTicked);
                card.setCommentsCount(commentsCount);

                double hoursFormatted = Math.round((workedSec / 3600.0) * 10.0) / 10.0;
                String summary = String.format("Logged %.1fh on this task. %d subtask%s completed, %d checkpoint%s ticked.",
                        hoursFormatted, subtasksDone, subtasksDone == 1 ? "" : "s", checkpointsTicked, checkpointsTicked == 1 ? "" : "s");
                if (workedSec == 0 && rangeEvents.isEmpty()) {
                    summary = "Task open during this period; no time or activity logged.";
                }
                card.setSummary(summary);

                taskCards.add(card);
            }
        }

        // Sort task cards: tasks with worked hours first, then alphabetically
        taskCards.sort((a, b) -> {
            if (b.getWorkedSeconds() != a.getWorkedSeconds()) {
                return Long.compare(b.getWorkedSeconds(), a.getWorkedSeconds());
            }
            return String.valueOf(a.getTitle()).compareToIgnoreCase(String.valueOf(b.getTitle()));
        });

        // 6. Compute summary numbers
        long totalWorkedSec = dayTotals.values().stream().mapToLong(Long::longValue).sum();
        double totalHours = Math.round((totalWorkedSec / 3600.0) * 100.0) / 100.0;
        int tasksWorkedOn = (int) taskCards.stream().filter(c -> c.getWorkedSeconds() > 0).count();
        int tasksCompleted = (int) taskCards.stream().filter(c -> c.getEvents().stream().anyMatch(e -> "TASK_DONE".equals(e.getEventType()))).count();
        int totalSubtasksCompleted = taskCards.stream().mapToInt(ReportTaskCardDto::getSubtasksCompletedCount).sum();
        int totalCheckpointsTicked = taskCards.stream().mapToInt(ReportTaskCardDto::getCheckpointsTickedCount).sum();

        ReportSummaryDto summary = new ReportSummaryDto(totalWorkedSec, totalHours, tasksWorkedOn, tasksCompleted, totalSubtasksCompleted, totalCheckpointsTicked);

        UserReportResponse response = new UserReportResponse();
        response.setUser(UserSummary.from(target)); // Excludes email and private data
        response.setFrom(from);
        response.setTo(to);
        response.setSummary(summary);
        response.setHoursPerDay(hoursPerDay);
        response.setTaskCards(taskCards);

        return response;
    }

    private void accumulateTime(Long taskId, Instant start, Instant end, LocalDate from, LocalDate to,
                                Map<LocalDate, Long> dayTotals, Map<Long, Long> taskTotals) {
        if (start == null || end == null || !start.isBefore(end)) return;

        LocalDate startDay = start.atZone(CAIRO).toLocalDate();
        LocalDate endDay = end.atZone(CAIRO).toLocalDate();

        LocalDate day = startDay;
        while (!day.isAfter(endDay)) {
            if (!day.isBefore(from) && !day.isAfter(to)) {
                Instant dayStart = day.atStartOfDay(CAIRO).toInstant();
                Instant nextDayStart = day.plusDays(1).atStartOfDay(CAIRO).toInstant();
                long overlapSec = overlap(start, end, dayStart, nextDayStart);
                if (overlapSec > 0) {
                    dayTotals.merge(day, overlapSec, Long::sum);
                    if (taskId != null) {
                        taskTotals.merge(taskId, overlapSec, Long::sum);
                    }
                }
            }
            day = day.plusDays(1);
        }
    }

    private static long overlap(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        Instant s = aStart.isAfter(bStart) ? aStart : bStart;
        Instant e = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return s.isBefore(e) ? Duration.between(s, e).toSeconds() : 0L;
    }
}
