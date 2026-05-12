package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import java.util.List;
import java.util.Optional;

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

    void acceptBooking(int bookingId, int callerId);

    void rejectBooking(int bookingId, int callerId);

    /**
     * If {@code payment.replyMsg != null}, this method will update {@code payment.repliedAt}.
     * Will always update {@code payment.createdAt}.
     * The rest is always uploaded to persistence.
     */
    void submitPayment(PaymentProof payment, int callerId);

    /** Payment proof bytes/metadata if {@code callerId} is the booking guest or listing host. */
    Optional<PaymentProof> getPaymentProofForParticipant(int bookingId, int callerId);

    void confirmPayment(int bookingId, int callerId);

    void rejectPayment(int bookingId, int callerId, String reason);

    void cancelBooking(int bookingId, int callerId);
}
