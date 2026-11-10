package com.taskmanagesys.task;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.service.FileViewTokenService;
import com.taskmanagesys.task.service.TaskFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileViewSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FileViewTokenService tokens;
    @MockBean TaskFileService files;

    @Test void validSignedContentWorksWithoutAuthorization() throws Exception {
        String token = tokens.issue(2, 10, 4);
        when(files.signedContent(10L, 4L, token)).thenReturn(new TaskFileService.ViewData(new byte[]{1,2,3}, "video/mp4", "clip.mp4", true));
        mvc.perform(get("/api/tasks/10/files/4/content").param("token", token)).andExpect(status().isOk());
    }
    @Test void tamperedExpiredAndOtherUserTokensFail() throws Exception {
        String token = tokens.issue(2, 10, 4);
        when(files.signedContent(eq(10L), eq(4L), anyString())).thenThrow(new AppException(HttpStatus.FORBIDDEN, "INVALID_FILE_VIEW", "invalid"));
        mvc.perform(get("/api/tasks/10/files/4/content").param("token", token + "x")).andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/10/files/4/content").param("token", tokens.issue(7, 10, 4))).andExpect(status().isForbidden());
    }
    @Test void everyOtherApiPathStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/tasks/10/files/4/metadata")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks/10/files")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks/10/files/4/zip-entries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks/10/files/4/text")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tasks/10/files/4/download")).andExpect(status().isUnauthorized());
    }
}
