package com.taskmanagesys.task;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.task.controller.TaskFileController;
import com.taskmanagesys.task.service.TaskFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskFileControllerTest {
    @Mock TaskFileService service; @Mock UserPrincipal user;

    @Test void imageDownloadIsInlineAndNosniff() {
        when(user.getId()).thenReturn(2L);
        when(service.download(10L, 4L, 2L)).thenReturn(new TaskFileService.Download(new ByteArrayResource(new byte[]{1}), "image/png", "photo.png", true));
        var response = new TaskFileController(service).download(10L, 4L, user);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("photo.png"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }
    @Test void nonImagePdfDownloadIsAttachmentAndNosniff() {
        when(user.getId()).thenReturn(2L);
        when(service.download(10L, 5L, 2L)).thenReturn(new TaskFileService.Download(new ByteArrayResource(new byte[]{1,2}), "application/octet-stream", "report.docx", false));
        var response = new TaskFileController(service).download(10L, 5L, user);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("report.docx"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }
    @Test void unicodeFilenameHasAsciiFallbackAndUtf8Parameter() {
        when(user.getId()).thenReturn(2L);
        when(service.download(10L, 7L, 2L)).thenReturn(new TaskFileService.Download(new ByteArrayResource(new byte[]{1}), "text/plain", "تقرير العمل.md", false));
        var response = new TaskFileController(service).download(10L, 7L, user);
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposition.startsWith("attachment"));
        assertTrue(disposition.contains("filename=\""));
        assertTrue(disposition.contains("filename*=UTF-8''"));
    }
    @Test void rangeContentReturnsPartialContentAndRangeHeader() {
        when(service.signedContent(10L, 4L, "ok")).thenReturn(new TaskFileService.ViewData(new byte[]{1,2,3,4}, "video/mp4", "clip.mp4", true));
        var response = new TaskFileController(service).content(10L, 4L, "ok", "bytes=1-2");
        assertEquals(206, response.getStatusCode().value());
        assertEquals("bytes 1-2/4", response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE));
        assertEquals("SAMEORIGIN", response.getHeaders().getFirst("X-Frame-Options"));
    }

    @Test void textResponseUsesUtf8AndReportsTruncation() {
        when(user.getId()).thenReturn(2L);
        when(service.textPreview(10L, 6L, 2L)).thenReturn(new TaskFileService.TextData("é\ntext", true));
        var response = new TaskFileController(service).text(10L, 6L, user);
        assertEquals("text/plain;charset=UTF-8", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals("true", response.getHeaders().getFirst("X-Content-Truncated"));
        assertEquals("é\ntext", response.getBody());
    }
}
