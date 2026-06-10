package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.booking.BookingBlockingStatuses;
import java.time.LocalDate;

/**
 * Inclusive search-filter date window for marketplace and landing filters. Keep
 * in sync with {@code visibleRangeEndIsoDate} in {@code date-time-picker.js}.
 */
public final class SearchDateRange {

    private SearchDateRange() {}

    public static LocalDate minInclusive() {
        return LocalDate.now();
    }

    public static LocalDate maxInclusive() {
        return LocalDate.now().plusDays(BookingBlockingStatuses.LISTING_PICKER_DAYS_AHEAD);
    }

    public static boolean isWithinRange(final LocalDate date) {
        if (date == null) {
            return true;
        }
        return !date.isBefore(minInclusive()) && !date.isAfter(maxInclusive());
    }
}
