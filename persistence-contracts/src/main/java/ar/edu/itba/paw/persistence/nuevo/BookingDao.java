package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import java.time.LocalDateTime;
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

    /** Bookings where the guest is the current user (outgoing requests). */
    BookingSearchResult searchOutcomingBookings(OutcomingSearch search);

    /** Bookings on listings owned by the host (incoming requests). */
    BookingSearchResult searchIncomingBookings(IncomingSearch search);

    void updateStatus(int id, BookingStatus status);

    void uploadPayment(PaymentProof paymentProof);

    void refusePayment(int bookingId, String message, LocalDateTime refuseTime);

    /** Set all bookings that ended before {@code maxEndTime} (UTC) as FINISHED */
    void finalizeBookingsBefore(LocalDateTime maxEndTime);

    /** Set all bookings that start after {@code minStartTime} (UTC) as CANCELLED */
    void expireBookingsAfter(LocalDateTime minStartTime);
}
