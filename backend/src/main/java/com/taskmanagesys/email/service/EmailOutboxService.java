package com.taskmanagesys.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.repository.EmailOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;

    public EmailOutboxService(EmailOutboxRepository emailOutboxRepository, ObjectMapper objectMapper) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void queueEmail(String toEmail, String template, Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            EmailOutbox email = new EmailOutbox(toEmail, template, jsonPayload);
            emailOutboxRepository.save(email);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize email payload", e);
        }
    }
}
