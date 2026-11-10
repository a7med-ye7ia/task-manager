package com.taskmanagesys.email.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.model.EmailStatus;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.email.sender.enabled", havingValue = "true", matchIfMissing = true)
public class EmailSenderJob {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderJob.class);

    private final EmailOutboxRepository emailOutboxRepository;
    private final MailService mailService;
    private final ObjectMapper objectMapper;
    private final EmailTemplateRenderer templateRenderer;

    public EmailSenderJob(
            EmailOutboxRepository emailOutboxRepository,
            MailService mailService,
            ObjectMapper objectMapper,
            EmailTemplateRenderer templateRenderer
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.mailService = mailService;
        this.objectMapper = objectMapper;
        this.templateRenderer = templateRenderer;
    }

    @Scheduled(fixedDelay = 30000)
    public void processOutbox() {
        List<EmailOutbox> pending = emailOutboxRepository.findPendingEmails(
                EmailStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, 50)
        );

        if (pending.isEmpty()) {
            return;
        }

        log.info("Processing {} pending email(s)", pending.size());
        for (EmailOutbox email : pending) {
            sendOne(email);
        }
    }

    @Transactional
    @org.springframework.scheduling.annotation.Async("mailTaskExecutor")
    public void sendOne(EmailOutbox email) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    email.getPayload(),
                    new TypeReference<Map<String, Object>>() {}
            );

            EmailTemplateRenderer.RenderedEmail content = templateRenderer.render(email.getTemplate(), payload);

            mailService.send(new MailService.Message(email.getToEmail(), content.subject(), content.html(), content.plainText()));

            email.setStatus(EmailStatus.SENT);
            email.setLastError(null);
            emailOutboxRepository.save(email);
            if (mailService.isRemoteDelivery()) {
                log.info("Sent email id={} template={} to={}", email.getId(), email.getTemplate(), email.getToEmail());
            } else {
                log.info("Email id={} template={} skipped because mail delivery is disabled", email.getId(), email.getTemplate());
            }

        } catch (Exception ex) {
            int attempts = email.getAttempts() + 1;
            email.setAttempts(attempts);
            email.setLastError(ex.getMessage());

            if (attempts >= 5) {
                email.setStatus(EmailStatus.FAILED);
                log.error("Email id={} permanently FAILED after {} attempts: {}", email.getId(), attempts, failureReason(ex));
            } else {
                long delayMinutes = switch (attempts) {
                    case 1 -> 1;
                    case 2 -> 5;
                    case 3 -> 15;
                    default -> 60;
                };
                email.setNextAttemptAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
                log.warn("Email id={} failed (attempt {}), next retry in {} min(s): {}",
                        email.getId(), attempts, delayMinutes, failureReason(ex));
            }
            emailOutboxRepository.save(email);
        }
    }

    private String failureReason(Exception ex) {
        StringBuilder details = new StringBuilder();
        for (Throwable current = ex; current != null; current = current.getCause()) {
            details.append(' ').append(current.toString());
        }
        String text = details.toString().toLowerCase();
        if (text.contains("authentication") || text.contains("535") || text.contains("auth fail")) {
            return "SMTP authentication failed: check the Gmail App Password";
        }
        if (text.contains("550") || text.contains("553") || text.contains("recipient")
                || text.contains("mailbox unavailable") || text.contains("address rejected")) {
            return "SMTP recipient was rejected: check the recipient address";
        }
        if (text.contains("connect") || text.contains("timeout") || text.contains("refused")) {
            return "SMTP connection failed: check the host, port and network";
        }
        return "SMTP delivery failed";
    }
}
