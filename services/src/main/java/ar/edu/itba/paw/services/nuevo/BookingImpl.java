package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingImpl implements BookingInterface {

    private final BookingDao bookingDao;

    @Override
    public PreBookingCreateResult createBooking(final PreBookingReq preBookingReq) {
        final int code = bookingDao.createBooking(preBookingReq);
        if (code > 0) {
            return new PreBookingCreateResult.Created(code);
        }
        if (code == BookingDao.RESULT_OUTSIDE_AVAILABILITY) {
            return PreBookingCreateResult.OutsideAvailability.INSTANCE;
        }
        if (code == BookingDao.RESULT_BOOKING_COLLISION) {
            return PreBookingCreateResult.Collision.INSTANCE;
        }
        return PreBookingCreateResult.Unexpected.INSTANCE;
    }

    @Override
    public List<Booking> getBookingsForVersion(final int versionId) {
        return bookingDao.getBookingsForVersion(versionId);
    }

    @Override
    public BookingSearchResult searchOutcomingBookings(final OutcomingSearch search) {
        return bookingDao.searchOutcomingBookings(search);
    }

    @Override
    public BookingSearchResult searchIncomingBookings(final IncomingSearch search) {
        return bookingDao.searchIncomingBookings(search);
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void updateStatus(Booking booking, BookingStatus status) {
        if (booking == null || status == null) return;
        bookingDao.updateStatus(booking.getId(), status);
    }

    public void acceptBooking(Booking booking) {
        updateStatus(booking, BookingStatus.ACCEPTED);
    }

    public void rejectBooking(Booking booking) {
        updateStatus(booking, BookingStatus.REJECTED);
    }

    public void submitPayment(PaymentProof payment) {
        if (payment == null) return;

        var now = currentDateTime();
        payment.setCreatedAt(now);
        if (payment.getReplyMsg() != null) payment.setRepliedAt(now);

        bookingDao.uploadPayment(payment);
        bookingDao.updateStatus(payment.getBookingId(), BookingStatus.PAID);
    }

    public void confirmPayment(Booking booking) {
        if (booking == null) return;

        bookingDao.updateStatus(booking.getId(), BookingStatus.CONFIRMED);
    }

    public void rejectPayment(int bookingId, String reason) {
        var now = currentDateTime();
        bookingDao.refusePayment(bookingId, reason, now);
        bookingDao.updateStatus(bookingId, BookingStatus.REFUSED);
    }
}
