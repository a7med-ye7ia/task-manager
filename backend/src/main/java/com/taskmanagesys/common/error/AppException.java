package com.taskmanagesys.common.error;

import org.springframework.http.HttpStatus;
import java.util.List;

public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorItem> fieldErrors;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.of();
    }

    public AppException(HttpStatus status, String code, String message, List<FieldErrorItem> fieldErrors) {
        super(message); this.status = status; this.code = code; this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorItem> getFieldErrors() { return fieldErrors; }
}
