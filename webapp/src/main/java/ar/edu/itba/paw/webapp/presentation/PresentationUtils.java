package ar.edu.itba.paw.webapp.presentation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class PresentationUtils {

    private PresentationUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    public static LocalDate parseDate(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    public static LocalTime parseTime(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value.trim(), REQUEST_TIME_FORMATTER);
    }
}
