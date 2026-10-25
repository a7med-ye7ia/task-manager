package com.taskmanagesys.email.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpMailService implements MailService {
    private final JavaMailSender sender;
    private final String from;
    private final String fromName;
    private final String replyTo;

    public SmtpMailService(JavaMailSender sender,
                           @Value("${app.mail.from}") String from,
                           @Value("${app.mail.from-name}") String fromName,
                           @Value("${app.mail.reply-to:}") String replyTo) {
        this.sender = sender;
        this.from = from;
        this.fromName = fromName;
        this.replyTo = replyTo;
    }

    @Override
    public void send(Message message) {
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(message.to());
            helper.setFrom(from, fromName);
            if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
            helper.setSubject(message.subject());
            helper.setText(message.text(), message.html());
            sender.send(mime);
        } catch (Exception ex) {
            throw new IllegalStateException("SMTP delivery failed", ex);
        }
    }
}
