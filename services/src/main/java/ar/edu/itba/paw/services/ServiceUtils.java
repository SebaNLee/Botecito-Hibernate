package ar.edu.itba.paw.services;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class ServiceUtils {

    private ServiceUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Converts a LocalDate to its corresponding DayOfWeek.
     *
     * @param date the Date to convert
     * @return the DayOfWeek, or null if date is null
     */
    public static DayOfWeek dateToDayOfWeek(final LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.getDayOfWeek();
    }

    public static String normalizeComment(final String comment) {
        if (comment == null) {
            return null;
        }
        final String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
