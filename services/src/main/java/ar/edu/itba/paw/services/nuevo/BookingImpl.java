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
        bookingDao.expireBookingsBefore(minStartTime);
    }

    private void verifyAnticipation(int bookingId) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
    }

    private void verifyAnticipation(final PreBookingReq request) {
        LocalDateTime start = LocalDateTime.of(request.getDate(), request.getStartTime());
        if (start.isBefore(currentMinimumStart())) throw new NoAnticipationException();
    }

    // TODO: send emails

    @Override
    public PreBookingCreateResult createBooking(final PreBookingReq preBookingReq) {
        verifyAnticipation(preBookingReq);
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
        verifyAnticipation(bookingId);
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatus.ACCEPTED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    public void rejectBooking(int bookingId, int callerId) {
        verifyAnticipation(bookingId);
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatus.REJECTED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    public void submitPayment(PaymentProof payment, int callerId) {
        if (payment == null) return;
        int bookingId = payment.getBookingId();

        verifyAnticipation(bookingId);

        bookingDao
                .updateStatusOutgoing(bookingId, callerId, BookingStatus.PAID)
                .orElseThrow(IllegalBookingOperationException::new);

        var now = currentDateTime();
        payment.setCreatedAt(now);
        if (payment.getReplyMsg() != null) payment.setRepliedAt(now);

        bookingDao.uploadPayment(payment).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    public Optional<PaymentProof> getPaymentProofForParticipant(final int bookingId, final int callerId) {
        return bookingDao.findPaymentProofForParticipant(bookingId, callerId);
    }

    @Override
    public void confirmPayment(int bookingId, int callerId) {
        verifyAnticipation(bookingId);
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatus.CONFIRMED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    public void rejectPayment(int bookingId, int callerId, String reason) {
        verifyAnticipation(bookingId);
        var now = currentDateTime();
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatus.REFUSED)
                .orElseThrow(IllegalBookingOperationException::new);
        bookingDao.refusePayment(bookingId, reason, now).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    public void cancelBooking(int bookingId, int callerId) {
        bookingDao
                .updateStatusOutgoing(bookingId, callerId, BookingStatus.CANCELLED)
                .orElseThrow(IllegalBookingOperationException::new);
    }
}
