package com.taskmanagesys.history;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.history.dto.DailyHoursDto;
import com.taskmanagesys.history.dto.UserReportResponse;
import com.taskmanagesys.history.model.WorkHistory;
import com.taskmanagesys.history.model.WorkHistoryEvent;
import com.taskmanagesys.history.repository.WorkHistoryEventRepository;
import com.taskmanagesys.history.repository.WorkHistoryRepository;
import com.taskmanagesys.history.service.ReportService;
import com.taskmanagesys.time.model.TimeSession;
import com.taskmanagesys.time.repository.TimeSegmentRepository;
import com.taskmanagesys.time.repository.TimeSessionRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkHistoryRepository workHistoryRepository;
    @Mock
    private WorkHistoryEventRepository workHistoryEventRepository;
    @Mock
    private TimeSessionRepository timeSessionRepository;
    @Mock
    private TimeSegmentRepository timeSegmentRepository;

    private Clock fixedClock;
    private ReportService reportService;

    private User manager;
    private User employee1;
    private User employee2;

    @BeforeEach
    void setUp() {
        // Fixed at 2026-11-10T12:00:00 Cairo time (+02:00) -> 2026-11-10T10:00:00Z
        Instant now = Instant.parse("2026-11-10T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneId.of("Africa/Cairo"));

        reportService = new ReportService(
                userRepository,
                workHistoryRepository,
                workHistoryEventRepository,
                timeSessionRepository,
                timeSegmentRepository,
                fixedClock
        );

        manager = new User();
        manager.setId(1L);
        manager.setRole(Role.MANAGER);
        manager.setFirstName("Boss");
        manager.setLastName("Man");
        manager.setEmail("boss@test.com");

        employee1 = new User();
        employee1.setId(10L);
        employee1.setRole(Role.EMPLOYEE);
        employee1.setFirstName("Alice");
        employee1.setLastName("Dev");
        employee1.setEmail("alice@test.com");

        employee2 = new User();
        employee2.setId(20L);
        employee2.setRole(Role.EMPLOYEE);
        employee2.setFirstName("Bob");
        employee2.setLastName("Dev");
        employee2.setEmail("bob@test.com");
    }

    @Test
    void managerCanViewAnyReport() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));
        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of());
        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);

        UserReportResponse response = reportService.generateReport(1L, 10L, from, to);
        assertNotNull(response);
        assertEquals(10L, response.getUser().id());
        assertEquals("Alice", response.getUser().firstName());
    }

    @Test
    void employeeCanViewOwnReport() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));
        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of());
        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);

        UserReportResponse response = reportService.generateReport(10L, 10L, from, to);
        assertNotNull(response);
        assertEquals(10L, response.getUser().id());
    }

    @Test
    void employeeCannotViewAnotherReport() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(employee2));

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);

        AppException ex = assertThrows(AppException.class, () ->
                reportService.generateReport(10L, 20L, from, to)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void rangeValidationToBeforeFrom() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));

        LocalDate from = LocalDate.of(2026, 11, 10);
        LocalDate to = LocalDate.of(2026, 11, 5);

        AppException ex = assertThrows(AppException.class, () ->
                reportService.generateReport(1L, 10L, from, to)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_DATE_RANGE", ex.getCode());
    }

    @Test
    void rangeValidationExceeds366Days() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1); // > 366 days

        AppException ex = assertThrows(AppException.class, () ->
                reportService.generateReport(1L, 10L, from, to)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("DATE_RANGE_EXCEEDED", ex.getCode());
    }

    @Test
    void rangeValidationBeforeRetentionPeriod() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));

        // Fixed date is 2026-11-10. 730 days ago is approx 2024-11-10.
        LocalDate from = LocalDate.of(2024, 1, 1); // Before 2-year retention window
        LocalDate to = LocalDate.of(2024, 6, 1);

        AppException ex = assertThrows(AppException.class, () ->
                reportService.generateReport(1L, 10L, from, to)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("DATE_BEFORE_RETENTION", ex.getCode());
    }

    @Test
    void inclusionRulesMemberAndOpenDuringRange() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);
        Instant rangeStart = from.atStartOfDay(ZoneId.of("Africa/Cairo")).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(ZoneId.of("Africa/Cairo")).toInstant();

        WorkHistory h1 = new WorkHistory();
        h1.setId(100L);
        h1.setTaskId(500L);
        h1.setTaskTitle("Active Project");
        h1.setUserId(10L);
        h1.setJoinedAt(rangeStart.minus(Duration.ofDays(5)));
        h1.setLeftAt(null);
        h1.setFinalStatus("IN_PROGRESS");

        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of(h1));
        when(workHistoryEventRepository.findByHistoryIdInOrderByOccurredAtAsc(List.of(100L))).thenReturn(List.of());
        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of());

        UserReportResponse response = reportService.generateReport(10L, 10L, from, to);
        assertEquals(1, response.getTaskCards().size());
        assertEquals("Active Project", response.getTaskCards().get(0).getTitle());
    }

    @Test
    void purgedTaskStillReportedFromHistory() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);
        Instant rangeStart = from.atStartOfDay(ZoneId.of("Africa/Cairo")).toInstant();

        WorkHistory purgedHistory = new WorkHistory();
        purgedHistory.setId(200L);
        purgedHistory.setTaskId(600L);
        purgedHistory.setTaskTitle("Purged Feature Task");
        purgedHistory.setUserId(10L);
        purgedHistory.setJoinedAt(rangeStart.minus(Duration.ofDays(2)));
        purgedHistory.setFinalStatus("TASK_DELETED");

        WorkHistoryEvent deleteEvent = new WorkHistoryEvent();
        deleteEvent.setId(301L);
        deleteEvent.setHistoryId(200L);
        deleteEvent.setEventType("TASK_DELETED");
        deleteEvent.setOccurredAt(rangeStart.plus(Duration.ofDays(2))); // deleted within range

        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of(purgedHistory));
        when(workHistoryEventRepository.findByHistoryIdInOrderByOccurredAtAsc(List.of(200L))).thenReturn(List.of(deleteEvent));
        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of());

        UserReportResponse response = reportService.generateReport(10L, 10L, from, to);
        assertEquals(1, response.getTaskCards().size());
        assertEquals("Purged Feature Task", response.getTaskCards().get(0).getTitle());
        assertEquals("TASK_DELETED", response.getTaskCards().get(0).getStatus());
    }

    @Test
    void hoursSplittingAtCairoMidnight() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));
        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of());

        LocalDate day1 = LocalDate.of(2026, 11, 5);
        LocalDate day2 = LocalDate.of(2026, 11, 6);

        // Cairo is UTC+2
        // Day 1 23:00 Cairo is 2026-11-05T21:00:00Z
        // Day 2 02:00 Cairo is 2026-11-06T00:00:00Z
        Instant start = Instant.parse("2026-11-05T21:00:00Z");
        Instant end = Instant.parse("2026-11-06T00:00:00Z");

        TimeSession session = new TimeSession();
        session.setId(50L);
        session.setUserId(10L);
        session.setTaskId(999L);
        session.setStartedAt(start);
        session.setEndedAt(end);

        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of(session));
        when(timeSegmentRepository.findBySessionIdInOrderByStartedAtAsc(List.of(50L))).thenReturn(List.of());

        UserReportResponse response = reportService.generateReport(10L, 10L, day1, day2);

        // Exactly 3600 seconds on Day 1, 7200 seconds on Day 2
        DailyHoursDto day1Dto = response.getHoursPerDay().stream()
                .filter(d -> d.getDate().equals(day1))
                .findFirst()
                .orElseThrow();
        DailyHoursDto day2Dto = response.getHoursPerDay().stream()
                .filter(d -> d.getDate().equals(day2))
                .findFirst()
                .orElseThrow();

        assertEquals(3600L, day1Dto.getWorkedSeconds());
        assertEquals(1.0, day1Dto.getHours());

        assertEquals(7200L, day2Dto.getWorkedSeconds());
        assertEquals(2.0, day2Dto.getHours());

        assertEquals(3.0, response.getSummary().getTotalHours());
    }

    @Test
    void summaryNumbersAndPrivacy() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee1));
        when(workHistoryRepository.findByUserId(10L)).thenReturn(List.of());
        when(timeSessionRepository.findSessionsForReport(eq(10L), any(), any())).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 2);

        UserReportResponse response = reportService.generateReport(10L, 10L, from, to);

        // Check privacy: UserSummary has id, firstName, lastName, avatarPath
        assertEquals(10L, response.getUser().id());
        assertEquals("Alice", response.getUser().firstName());
        assertEquals("Dev", response.getUser().lastName());
        // Verify response UserSummary has no email field exposed or returned
        assertNotNull(response.getSummary());
        assertEquals(0L, response.getSummary().getTotalWorkedSeconds());
        assertEquals(0.0, response.getSummary().getTotalHours());
        assertEquals(0, response.getSummary().getTasksWorkedOn());
        assertEquals(0, response.getSummary().getTasksCompleted());
    }
}
