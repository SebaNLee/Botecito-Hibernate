package ar.edu.itba.paw.models.booking;

import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for which booking statuses block availability display vs collision checks.
 * Display blocking excludes {@link BookingStatusEnum#REFUSED} because refused bookings free the slot in the UI.
 */
public final class BookingBlockingStatuses {

    public static final int BOOKING_CLEARANCE_MINUTES = 30;
    public static final int LISTING_PICKER_MONTHS_AROUND_TODAY = 2;
    public static final int TIME_SLOT_STEP_MINUTES = 30;

    private static final Set<BookingStatusEnum> DISPLAY_BLOCKING = EnumSet.of(
            BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED, BookingStatusEnum.PAID, BookingStatusEnum.CONFIRMED);

    private static final EnumSet<BookingStatusEnum> COLLISION_BLOCKING = EnumSet.of(
            BookingStatusEnum.PENDING,
            BookingStatusEnum.ACCEPTED,
            BookingStatusEnum.PAID,
            BookingStatusEnum.REFUSED,
            BookingStatusEnum.CONFIRMED);

    private BookingBlockingStatuses() {}

    public static boolean isDisplayBlocking(final BookingStatusEnum status) {
        return status != null && DISPLAY_BLOCKING.contains(status);
    }

    public static EnumSet<BookingStatusEnum> collisionBlockingStates() {
        return EnumSet.copyOf(COLLISION_BLOCKING);
    }

    /** SQL literal list for native queries, e.g. {@code CAST('PENDING' AS booking_status_enum), ...}. */
    public static String displayBlockingStatusesSqlLiteral() {
        return DISPLAY_BLOCKING.stream()
                .map(status -> "CAST('" + status.name() + "' AS booking_status_enum)")
                .collect(Collectors.joining(", "));
    }
}
