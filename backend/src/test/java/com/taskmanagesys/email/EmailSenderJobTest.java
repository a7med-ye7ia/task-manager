package com.taskmanagesys.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import com.taskmanagesys.email.service.EmailSenderJob;
import com.taskmanagesys.email.service.EmailTemplateRenderer;
import com.taskmanagesys.email.service.MailService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailSenderJobTest {
    @Test
    void deliveryFailureIsRecordedWithoutEscapingTheJob() throws Exception {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        MailService mailService = mock(MailService.class);
        ObjectMapper mapper = new ObjectMapper();
        EmailTemplateRenderer renderer = mock(EmailTemplateRenderer.class);
        EmailSenderJob job = new EmailSenderJob(repository, mailService, mapper, renderer);
        EmailOutbox outbox = new EmailOutbox("employee@example.com", "UNKNOWN", mapper.writeValueAsString(Map.of()));
        doThrow(new IllegalStateException("connection refused")).when(mailService).send(any());

        assertDoesNotThrow(() -> job.sendOne(outbox));

        assertEquals(1, outbox.getAttempts());
        assertEquals("connection refused", outbox.getLastError());
        verify(repository).save(outbox);
    }
}
