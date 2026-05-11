package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchModel;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import java.util.List;

public interface BookingDao {

    /** Selected interval is not fully covered by a single availability row. */
    int RESULT_OUTSIDE_AVAILABILITY = -1;

    /**
     * Interval is valid for availability but overlaps another blocking booking
     * (including clearance).
     */
    int RESULT_BOOKING_COLLISION = -2;

    /** Invalid/unexpected booking request or persistence failure. */
    int RESULT_UNEXPECTED_ERROR = -3;

    /**
     * Inserts a {@code PENDING} booking when the request is valid, the interval
     * lies entirely inside
     * one availability window for the version (interpreted in
     * {@code version.timezone}), and UTC
     * bounds do not collide with another blocking booking on the same version
     * (including the
     * configured minute gap).
     *
     * @return new booking id (positive), {@link #RESULT_OUTSIDE_AVAILABILITY}, or
     *         {@link
     *         #RESULT_BOOKING_COLLISION}, or {@link #RESULT_UNEXPECTED_ERROR}
     */
    int createBooking(PreBookingReq preBookingReq);

    /** Bookings for the given listing version, ordered by start time (UTC). */
    List<Booking> getBookingsForVersion(int versionId);

    BookingSearchResult searchBookings(BookingSearchModel search);
}
