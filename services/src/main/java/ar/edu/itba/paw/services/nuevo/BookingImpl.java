package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.models.nuevo.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.nuevo.exceptions.NoAnticipationException;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingImpl implements BookingInterface {

    private final BookingDao bookingDao;

    private static final int MIN_ANTICIPATION_MINUTES = 120;

    // TODO: move current times to utils

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime currentMinimumStart() {
        return currentDateTime().plusMinutes(MIN_ANTICIPATION_MINUTES);
    }

    private boolean hasEnoughAnticipation(int bookingId) {
        LocalDateTime minStartTime = currentMinimumStart();
        return bookingDao.startsAfter(bookingId, minStartTime);
    }

    private void finalizeBookings() {
        bookingDao.finalizeBookingsBefore(currentDateTime());
    }

    private void expireDueBookings() {
        LocalDateTime minStartTime = currentMinimumStart();
        bookingDao.expireBookingsAfter(minStartTime);
    }

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

    @Scheduled(cron = "0 0,30 * * * *")
    public void bookingResolutionRoutine() {
        finalizeBookings();
        expireDueBookings();
    }

    @Override
    public void acceptBooking(int bookingId, int callerId) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
        boolean success = bookingDao.updateStatusIncoming(bookingId, callerId, BookingStatus.ACCEPTED);
        if (!success) throw new IllegalBookingOperationException();
    }

    @Override
    public void rejectBooking(int bookingId, int callerId) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
        boolean success = bookingDao.updateStatusIncoming(bookingId, callerId, BookingStatus.REJECTED);
        if (!success) throw new IllegalBookingOperationException();
    }

    @Override
    public void submitPayment(PaymentProof payment, int callerId) {
        if (payment == null) return;
        int bookingId = payment.getBookingId();

        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();

        boolean success = bookingDao.updateStatusOutgoing(bookingId, callerId, BookingStatus.PAID);
        if (!success) throw new IllegalBookingOperationException();

        var now = currentDateTime();
        payment.setCreatedAt(now);
        if (payment.getReplyMsg() != null) payment.setRepliedAt(now);

        bookingDao.uploadPayment(payment);
    }

    @Override
    public Optional<PaymentProof> getPaymentProofForParticipant(final int bookingId, final int callerId) {
        return bookingDao.findPaymentProofForParticipant(bookingId, callerId);
    }

    @Override
    public void confirmPayment(int bookingId, int callerId) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
        boolean success = bookingDao.updateStatusIncoming(bookingId, callerId, BookingStatus.CONFIRMED);
        if (!success) throw new IllegalBookingOperationException();
    }

    @Override
    public void rejectPayment(int bookingId, int callerId, String reason) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
        var now = currentDateTime();
        final boolean statusUpdated = bookingDao.updateStatusIncoming(bookingId, callerId, BookingStatus.REFUSED);
        if (!statusUpdated) {
            throw new IllegalBookingOperationException();
        }
        bookingDao.refusePayment(bookingId, reason, now);
    }

    @Override
    public void cancelBooking(int bookingId, int callerId) {
        boolean success = bookingDao.updateStatusOutgoing(bookingId, callerId, BookingStatus.CANCELLED);
        if (!success) throw new IllegalBookingOperationException();
    }
}
