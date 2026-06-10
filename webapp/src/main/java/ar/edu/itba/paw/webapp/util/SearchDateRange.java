package ar.edu.itba.paw.webapp.util;

import java.time.LocalDate;

/**
 * Inclusive search-filter date window for marketplace and landing filters. Keep
 * in sync with
 * {@code visibleRangeEndIsoDate} in {@code date-time-picker.js}.
 */
public final class SearchDateRange {

    /** Months ahead of today for the inclusive maximum filter date. */
    public static final int MONTHS_AHEAD = 2;

    private SearchDateRange() {}

    public static LocalDate minInclusive() {
        return LocalDate.now();
    }

    public static LocalDate maxInclusive() {
        return LocalDate.now().plusMonths(MONTHS_AHEAD);
    }

    public static boolean isWithinRange(final LocalDate date) {
        if (date == null) {
            return true;
        }
        return !date.isBefore(minInclusive()) && !date.isAfter(maxInclusive());
    }
}
