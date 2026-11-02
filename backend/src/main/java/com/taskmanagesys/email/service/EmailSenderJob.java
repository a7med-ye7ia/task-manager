package com.taskmanagesys.email.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.model.EmailStatus;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

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

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public EmailSenderJob(EmailOutboxRepository emailOutboxRepository, MailService mailService, ObjectMapper objectMapper) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.mailService = mailService;
        this.objectMapper = objectMapper;
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

            EmailContent content = buildContent(email.getTemplate(), payload);

            mailService.send(new MailService.Message(email.getToEmail(), content.subject, content.html, content.plainText));

            email.setStatus(EmailStatus.SENT);
            email.setLastError(null);
            emailOutboxRepository.save(email);
            log.info("Sent email id={} template={} to={}", email.getId(), email.getTemplate(), email.getToEmail());

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
        if (text.contains("connect") || text.contains("timeout") || text.contains("refused")) {
            return "SMTP connection failed: check the host, port and network";
        }
        return "SMTP delivery failed";
    }

    private EmailContent buildContent(String template, Map<String, Object> payload) {
        String appName = "Task Management System";
        return switch (template) {
            case "ACCOUNT_INVITE" -> {
                String token = (String) payload.get("token");
                String link = appBaseUrl + "/set-password?token=" + token;
                String sentence = "You have been invited to join the " + appName + ". Click the button below to set your password.";
                yield new EmailContent(
                        "Invitation to join " + appName,
                        formatHtml(appName, sentence, link, "Set your password", "This invitation link is valid for 48 hours."),
                        sentence + "\nSet your password: " + link + "\nValid for 48 hours."
                );
            }
            case "VERIFY_EMAIL" -> {
                String token = (String) payload.get("token");
                String link = appBaseUrl + "/verify-email?token=" + token;
                String sentence = "Thank you for registering. Please verify your email address to submit your account for Manager approval.";
                yield new EmailContent(
                        "Verify your email address",
                        formatHtml(appName, sentence, link, "Verify Email", "This verification link is valid for 24 hours."),
                        sentence + "\nVerify your email: " + link + "\nValid for 24 hours."
                );
            }
            case "CONFIRM_NEW_EMAIL" -> {
                String token = (String) payload.get("token");
                String link = appBaseUrl + "/verify-email?token=" + token;
                String sentence = "A request was made to update your email address. Please click the button below to confirm this address.";
                yield new EmailContent(
                        "Confirm your new email address",
                        formatHtml(appName, sentence, link, "Confirm New Email", "This link is valid for 24 hours."),
                        sentence + "\nConfirm email: " + link + "\nValid for 24 hours."
                );
            }
            case "NEW_SIGNUP_REQUEST" -> {
                String applicantName = HtmlUtils.htmlEscape((String) payload.getOrDefault("name", "An applicant"));
                String applicantEmail = HtmlUtils.htmlEscape((String) payload.getOrDefault("email", ""));
                String link = appBaseUrl + "/admin/pending";
                String sentence = applicantName + " (" + applicantEmail + ") has verified their email and is waiting for account approval.";
                yield new EmailContent(
                        "New sign-up request pending approval",
                        formatHtml(appName, sentence, link, "Review Requests", "Review and approve or reject in the Manager portal."),
                        sentence + "\nReview requests: " + link
                );
            }
            case "SIGNUP_APPROVED" -> {
                String link = appBaseUrl + "/login";
                String sentence = "Your account registration has been approved by a Manager. You can now sign in.";
                yield new EmailContent(
                        "Your sign-up request was approved",
                        formatHtml(appName, sentence, link, "Sign In to Your Account", ""),
                        sentence + "\nSign in: " + link
                );
            }
            case "SIGNUP_REJECTED" -> {
                String reason = (String) payload.get("reason");
                String escapedReason = (reason != null && !reason.trim().isEmpty())
                        ? " Reason: " + HtmlUtils.htmlEscape(reason.trim())
                        : "";
                String sentence = "Your account registration request was reviewed and rejected." + escapedReason;
                yield new EmailContent(
                        "Your sign-up request was rejected",
                        formatHtml(appName, sentence, null, null, "If you believe this was an error, please contact your manager."),
                        sentence
                );
            }
            case "PASSWORD_RESET" -> {
                String token = (String) payload.get("token");
                String link = appBaseUrl + "/reset-password?token=" + token;
                String sentence = "A password reset was requested for your account. Click the button below to choose a new password.";
                yield new EmailContent(
                        "Password reset request",
                        formatHtml(appName, sentence, link, "Reset Password", "This link is valid for 30 minutes. If you did not request this, please ignore this email."),
                        sentence + "\nReset password: " + link + "\nValid for 30 minutes."
                );
            }
            case "PASSWORD_CHANGED" -> {
                String sentence = "The password for your account has been changed successfully. All other sessions have been signed out.";
                yield new EmailContent(
                        "Your password was changed",
                        formatHtml(appName, sentence, appBaseUrl + "/login", "Sign In", "If you did not make this change, please contact a manager immediately."),
                        sentence + "\nSign in: " + appBaseUrl + "/login"
                );
            }
            case "ACCOUNT_ALREADY_EXISTS" -> {
                String sentence = "Someone attempted to sign up for an account using your email address. If this was you, you can sign in directly or reset your password.";
                yield new EmailContent(
                        "Security notification: sign-up attempt",
                        formatHtml(appName, sentence, appBaseUrl + "/login", "Go to Sign In", "If this was not you, no action is required."),
                        sentence + "\nSign in: " + appBaseUrl + "/login"
                );
            }
            default -> new EmailContent("Notification from " + appName, "<p>You have a new notification.</p>", "You have a new notification.");
        };
    }

    private String formatHtml(String appName, String sentence, String buttonLink, String buttonText, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px;\">");
        sb.append("<div style=\"max-width: 550px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; padding: 25px; background: #ffffff;\">");
        sb.append("<h2 style=\"color: #2563eb; margin-top: 0;\">").append(HtmlUtils.htmlEscape(appName)).append("</h2>");
        sb.append("<p style=\"font-size: 16px; margin: 20px 0;\">").append(sentence).append("</p>");
        if (buttonLink != null && buttonText != null) {
            sb.append("<div style=\"margin: 30px 0;\">");
            sb.append("<a href=\"").append(buttonLink).append("\" style=\"background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">")
              .append(HtmlUtils.htmlEscape(buttonText))
              .append("</a></div>");
        }
        if (note != null && !note.isEmpty()) {
            sb.append("<p style=\"font-size: 13px; color: #64748b; margin-top: 25px; border-top: 1px solid #f1f5f9; padding-top: 15px;\">")
              .append(HtmlUtils.htmlEscape(note))
              .append("</p>");
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private record EmailContent(String subject, String html, String plainText) {}
}
