package com.taskmanagesys.common.validation;

import com.taskmanagesys.common.error.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsPasswordTest {

    @Test
    @DisplayName("Valid password with special character passes validation")
    void validPasswordWithSpecialCharPasses() {
        assertDoesNotThrow(() -> ValidationUtils.validatePassword("ValidPass123!", "user@gmail.com"));
        assertDoesNotThrow(() -> ValidationUtils.validatePassword("Strong@P4ssword#", "user@gmail.com"));
        assertDoesNotThrow(() -> ValidationUtils.validatePassword("MyPassword2026$", "user@gmail.com"));
    }

    @Test
    @DisplayName("Password without special character fails validation with expected message")
    void passwordWithoutSpecialCharFails() {
        AppException ex = assertThrows(AppException.class,
                () -> ValidationUtils.validatePassword("NoSpecial123", "user@gmail.com"));
        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertTrue(ex.getMessage().contains("special character"));
    }

    @Test
    @DisplayName("Password with spaces, missing digits, or too short fails")
    void invalidPasswordVariationsFail() {
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword(" Short1!", "user@gmail.com"));
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword("Short1! ", "user@gmail.com"));
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword("NoDigit!", "user@gmail.com"));
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword("NOLOWER123!", "user@gmail.com"));
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword("noupper123!", "user@gmail.com"));
        assertThrows(AppException.class, () -> ValidationUtils.validatePassword("User@gmail.com!", "user@gmail.com"));
    }
}
