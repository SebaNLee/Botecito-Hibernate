package ar.edu.itba.paw.models.booking;

import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import java.util.EnumSet;

/**
 * Single source of truth for which booking statuses block availability display vs collision checks.
 * Display blocking excludes {@link BookingStatusEnum#REFUSED} because refused bookings free the slot in the UI.
 */
public final class BookingBlockingStatuses {

    public static final int BOOKING_CLEARANCE_MINUTES = 30;
    public static final int LISTING_PICKER_DAYS_AHEAD = 60;
    public static final int TIME_SLOT_STEP_MINUTES = 30;

    private static final EnumSet<BookingStatusEnum> DISPLAY_BLOCKING = EnumSet.of(
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

    public static EnumSet<BookingStatusEnum> displayBlockingStates() {
        return EnumSet.copyOf(DISPLAY_BLOCKING);
    }

    public static EnumSet<BookingStatusEnum> collisionBlockingStates() {
        return EnumSet.copyOf(COLLISION_BLOCKING);
    }
}
