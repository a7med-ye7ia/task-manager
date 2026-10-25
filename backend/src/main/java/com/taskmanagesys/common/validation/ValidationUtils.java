package com.taskmanagesys.common.validation;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.common.error.FieldErrorItem;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} '-]{2,50}$");
    private static final Pattern GMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@gmail\\.com$");
    private static final Pattern EGYPT_PHONE_PATTERN = Pattern.compile("^(?:\\+20|0)?1[0125]\\d{8}$");
    private static final Pattern E164_PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    private ValidationUtils() {}

    public static String validateAndNormalizeName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", fieldName + " is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50 || !NAME_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    fieldName + " must be 2 to 50 characters and contain only letters, spaces, hyphens, or apostrophes");
        }
        return trimmed;
    }

    public static String validateAndNormalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Email is required");
        }
        String trimmed = email.trim().toLowerCase();
        if (trimmed.length() > 254 || !GMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Email must be a valid Gmail address ending in @gmail.com (up to 254 characters)");
        }
        return trimmed;
    }

    public static String validateAndNormalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Phone is required");
        }
        String cleaned = phone.trim().replaceAll("\\s+", "");
        if (EGYPT_PHONE_PATTERN.matcher(cleaned).matches()) {
            if (cleaned.startsWith("+20")) {
                return cleaned;
            } else if (cleaned.startsWith("0")) {
                return "+20" + cleaned.substring(1);
            } else if (cleaned.startsWith("1")) {
                return "+20" + cleaned;
            }
        }
        if (E164_PHONE_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Phone must be a valid Egyptian mobile number (e.g. 01012345678) or E.164 international format");
    }

    public static void validatePassword(String password, String email) {
        if (password == null || password.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Password is required");
        }
        if (password.startsWith(" ") || password.endsWith(" ")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must not contain leading or trailing spaces");
        }
        if (password.length() < 8 || password.length() > 64) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be between 8 and 64 characters");
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must contain at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must contain at least one digit");
        }
        if (email != null && password.equalsIgnoreCase(email.trim())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password cannot be equal to your email address");
        }
    }
}
