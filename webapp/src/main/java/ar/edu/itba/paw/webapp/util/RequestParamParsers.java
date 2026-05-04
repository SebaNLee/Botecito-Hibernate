package ar.edu.itba.paw.webapp.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public final class RequestParamParsers {

    private RequestParamParsers() {}

    public static Integer parseInteger(final String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    public static LocalDate parseLocalDate(final String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    public static LocalTime parseLocalTime(final String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
