package com.taskmanagesys.common.error;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private List<FieldErrorItem> fieldErrors = new ArrayList<>();
    private Instant timestamp = Instant.now();
    private String path;
    private String traceId;

    public ErrorResponse() {}

    public ErrorResponse(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.fieldErrors = new ArrayList<>();
    }

    public ErrorResponse(int status, String code, String message, List<FieldErrorItem> fieldErrors) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorItem> fieldErrors) {
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
