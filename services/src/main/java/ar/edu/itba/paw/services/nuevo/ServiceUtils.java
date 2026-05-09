package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;

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

    public static boolean isReviewWindowOpen(final ItemBooking booking) {
        if (booking == null || booking.getState() == null || booking.getEndTime() == null) {
            return false;
        }
        return isBookingEligibleForPostStayReview(booking.getState())
                && booking.getEndTime().isBefore(OffsetDateTime.now());
    }

    public static boolean isBookingEligibleForPostStayReview(final BookingState state) {
        return switch (state) {
            case BOOKING_CONFIRMED, BOOKING_PAYMENT_SUBMITTED, BOOKING_PAID, BOOKING_COMPLETED -> true;
            default -> false;
        };
    }
}
