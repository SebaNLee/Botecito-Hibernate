package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import java.util.List;

public interface BookingInterface {

    /**
     * Creates a pre-booking; interprets
     * {@link ar.edu.itba.paw.persistence.nuevo.BookingDao} return
     * codes into a {@link PreBookingCreateResult}.
     */
    PreBookingCreateResult createBooking(PreBookingReq preBookingReq);

    List<Booking> getBookingsForVersion(int versionId);

    /** Bookings where {@link OutcomingSearch#getGuestId()} is the guest (sent requests). */
    BookingSearchResult searchOutcomingBookings(OutcomingSearch search);

    /** Bookings on items where {@link IncomingSearch#getHostId()} is the host (received requests). */
    BookingSearchResult searchIncomingBookings(IncomingSearch search);

    void acceptBooking(Booking booking);

    void rejectBooking(Booking booking);

    /**
     * If {@code payment.replyMsg != null}, this method will update {@code payment.repliedAt}.
     * Will always update {@code payment.createdAt}.
     * The rest is always uploaded to persistence.
     */
    void submitPayment(PaymentProof payment);

    void confirmPayment(Booking booking);

    void rejectPayment(int bookingId, String reason);

    public void cancelBooking(Booking booking);
}
