package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.regex.Pattern;

final class ServiceInputValidator {
    static final int NAME_MAX_LENGTH = 100;
    static final int EMAIL_MAX_LENGTH = 150;
    static final int PASSWORD_MIN_LENGTH = 8;
    static final int PASSWORD_MAX_LENGTH = 100;
    static final int TITLE_MAX_LENGTH = 100;
    static final int DESCRIPTION_MAX_LENGTH = 1000;
    static final int CLASS_USER_FIELD_MAX_LENGTH = 255;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ServiceInputValidator() {}

    static String requireText(final String value, final String fieldName, final int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        final String trimmed = value.trim();
        requireMaxLength(trimmed, fieldName, maxLength);
        return trimmed;
    }

    static String optionalText(final String value, final String fieldName, final int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        final String trimmed = value.trim();
        requireMaxLength(trimmed, fieldName, maxLength);
        return trimmed;
    }

    static String requireEmail(final String email) {
        final String normalizedEmail =
                requireText(email, "email", EMAIL_MAX_LENGTH).toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("email is invalid");
        }
        return normalizedEmail;
    }

    static void requirePassword(final String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("password must be between 8 and 100 characters");
        }
    }

    static int requirePositive(final Integer value, final String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    static int requireNonNegative(final Integer value, final String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    static BigDecimal requirePositiveIfPresent(final BigDecimal value, final String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    static void requireMaxLength(final String value, final String fieldName, final int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
    }
}
