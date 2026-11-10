package com.taskmanagesys.common;

import com.taskmanagesys.common.error.ErrorResponse;
import com.taskmanagesys.common.error.GlobalExceptionHandler;
import com.taskmanagesys.config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    @Test void unexpectedErrorsReturnReferenceAndKeepTraceInLogContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setAttribute(TraceIdFilter.ATTRIBUTE, "trace-123");
        ErrorResponse body = new GlobalExceptionHandler().handleGeneralException(new IllegalStateException("secret detail"), new ServletWebRequest(request)).getBody();
        assertEquals(500, body.getStatus());
        assertEquals("Something went wrong. Try again.", body.getMessage());
        assertEquals("trace-123", body.getTraceId());
    }
}
