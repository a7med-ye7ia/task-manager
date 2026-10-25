package com.taskmanagesys.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.audit.model.AuditLog;
import com.taskmanagesys.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long actorId, Long targetUserId, String action, Map<String, Object> details) {
        String json = null;
        if (details != null && !details.isEmpty()) {
            try {
                json = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException ignored) {
                json = "{}";
            }
        }
        AuditLog entry = new AuditLog(actorId, targetUserId, action, json);
        auditLogRepository.saveAndFlush(entry);
    }
}
