package com.taskmanagesys.time;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.common.dto.PageResponse;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.time.controller.TimeSessionController;
import com.taskmanagesys.time.dto.TimeTaskOptionResponse;
import com.taskmanagesys.time.service.TimeSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeTaskOptionsControllerTest {
    @Mock TimeSessionService service;
    @Mock UserPrincipal user;

    @Test void taskOptionsExposeOnlyMinimalPagedOptionFields() {
        when(user.getId()).thenReturn(8L);
        var option = new TimeTaskOptionResponse(12L, "Prepare report", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 11, 20));
        when(service.taskOptions(eq(8L), eq("report"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(option), PageRequest.of(1, 20), 21));
        TimeSessionController controller = new TimeSessionController(service);

        PageResponse<TimeTaskOptionResponse> response = controller.taskOptions(user, "report", 1);

        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(21, response.totalElements());
        assertEquals(12L, response.content().get(0).id());
        verify(service).taskOptions(eq(8L), eq("report"), any(PageRequest.class));
    }
}
