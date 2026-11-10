package com.taskmanagesys.common.error;

import com.taskmanagesys.config.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // BindException is already mapped by ResponseEntityExceptionHandler.handleException, so it must NOT get its own
    // @ExceptionHandler here (that fails at startup with "Ambiguous @ExceptionHandler method mapped").
    // Real validation failures arrive as MethodArgumentNotValidException (a BindException subclass) and are handled here.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(status.value(), "VALIDATION_ERROR", "Validation failed for one or more fields", fields(ex.getBindingResult().getFieldErrors()), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(400, "MALFORMED_JSON", "The request body is invalid", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(400, "MISSING_PARAMETER", "Required parameter is missing: " + ex.getParameterName(), List.of(new FieldErrorItem(ex.getParameterName(), "Required")), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(org.springframework.beans.TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : "request";
        return response(400, "TYPE_MISMATCH", "The request contains an invalid value", List.of(new FieldErrorItem(field, "Invalid value")), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(405, "METHOD_NOT_ALLOWED", "Method not allowed", List.of(), request);
    }

    // These two are already mapped by ResponseEntityExceptionHandler itself. Declaring them again with
    // @ExceptionHandler makes Spring fail at startup with "Ambiguous @ExceptionHandler method mapped",
    // so they must be overrides.
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(404, "NOT_FOUND", "The requested resource was not found", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(404, "NOT_FOUND", "The requested resource was not found", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return response(413, "PAYLOAD_TOO_LARGE", "The uploaded file is too large", List.of(), request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex, WebRequest request) {
        return typedResponse(400, "MULTIPART_ERROR", "The upload could not be processed", List.of(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream().map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage())).toList();
        return typedResponse(400, "CONSTRAINT_VIOLATION", "Constraint violation error", errors, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return typedResponse(409, "CONFLICT", "The request conflicts with existing data", List.of(), request);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, WebRequest request) {
        return typedResponse(ex.getStatus().value(), ex.getCode(), ex.getMessage(), ex.getFieldErrors(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        return typedResponse(ex.getStatusCode().value(), ex.getStatusCode().toString(), ex.getReason() != null ? ex.getReason() : "Request failed", List.of(), request);
    }

    // Without this, the catch-all below would turn a 401/403 thrown inside a controller or service
    // (for example by @PreAuthorize) into a 500. Rethrowing hands it back to Spring Security's filter
    // chain, whose entry point and access-denied handler already return the standard JSON error.
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(Exception ex) throws Exception {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
        String traceId = traceId(request);
        log.error("Unhandled exception traceId={}", traceId, ex);
        return typedResponse(500, "INTERNAL_SERVER_ERROR", "Something went wrong. Try again.", List.of(), request);
    }

    private List<FieldErrorItem> fields(List<FieldError> errors) {
        return errors.stream().map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage())).toList();
    }

    private ResponseEntity<Object> response(int status, String code, String message, List<FieldErrorItem> fields, WebRequest request) {
        return ResponseEntity.status(status).body(body(status, code, message, fields, request));
    }

    private ResponseEntity<ErrorResponse> typedResponse(int status, String code, String message, List<FieldErrorItem> fields, WebRequest request) {
        return ResponseEntity.status(status).body(body(status, code, message, fields, request));
    }

    private ErrorResponse body(int status, String code, String message, List<FieldErrorItem> fields, WebRequest request) {
        ErrorResponse body = new ErrorResponse(status, code, message, fields);
        body.setPath(request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null);
        body.setTraceId(traceId(request));
        return body;
    }

    private String traceId(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            Object value = servlet.getRequest().getAttribute(TraceIdFilter.ATTRIBUTE);
            return value == null ? null : value.toString();
        }
        return null;
    }
}
