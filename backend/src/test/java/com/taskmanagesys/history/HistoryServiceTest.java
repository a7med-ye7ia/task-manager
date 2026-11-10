package com.taskmanagesys.history;

import com.taskmanagesys.history.repository.WorkHistoryEventRepository;
import com.taskmanagesys.history.repository.WorkHistoryRepository;
import com.taskmanagesys.history.service.HistoryService;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMember;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {
    @Mock WorkHistoryRepository histories; @Mock WorkHistoryEventRepository events; @Mock TaskRepository tasks; @Mock TaskMemberRepository members;
    @Test void taskEventsCreateDurableHistoryRowsInTheSameCall() {
        HistoryService service = new HistoryService(histories, events, tasks, members, Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC));
        Task task = new Task(); task.setId(4L); task.setTitle("History task"); task.setStatus(TaskStatus.IN_PROGRESS);
        TaskMember member = new TaskMember(); member.setTaskId(4L); member.setUserId(8L); member.setRole(TaskMemberRole.MEMBER);
        when(tasks.findById(4L)).thenReturn(Optional.of(task)); when(members.findByTaskId(4L)).thenReturn(List.of(member)); when(histories.findByUserIdAndTaskId(8L,4L)).thenReturn(Optional.empty()); when(histories.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.recordTaskEvent(4L, "SUBTASK_DONE", java.util.Map.of("subtaskTitle", "First"));
        verify(histories).save(any()); verify(events).save(any());
    }

    @Test void taskDeletionClosesHistoryAndWritesIndependentEvent() {
        HistoryService service = new HistoryService(histories, events, tasks, members, Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC));
        Task task = new Task(); task.setId(4L); task.setTitle("Deleted task"); task.setStatus(TaskStatus.DONE);
        TaskMember member = new TaskMember(); member.setTaskId(4L); member.setUserId(8L); member.setRole(TaskMemberRole.MEMBER);
        when(members.findByTaskId(4L)).thenReturn(List.of(member)); when(histories.findByUserIdAndTaskId(8L,4L)).thenReturn(Optional.empty()); when(histories.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.recordTaskDeleted(task, 2L);
        verify(events).save(any()); verify(histories).save(any());
    }

    @Test void retentionDeletesEventsBeforeHistoryRows() {
        Instant cutoff = Instant.parse("2024-11-01T00:00:00Z"); when(histories.deleteByJoinedAtBefore(cutoff)).thenReturn(3L); when(events.deleteByOccurredAtBefore(cutoff)).thenReturn(4L);
        HistoryService service = new HistoryService(histories, events, tasks, members, Clock.systemUTC());
        service.cleanup(cutoff);
        verify(events).deleteByOccurredAtBefore(cutoff); verify(histories).deleteByJoinedAtBefore(cutoff);
    }
}
