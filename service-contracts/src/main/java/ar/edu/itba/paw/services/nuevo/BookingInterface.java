package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.PaymentProofOrm;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.nuevo.OwnerAvailabilityPage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingInterface {

    int createBooking(int versionId, LocalDate date, LocalTime startTime,
            LocalTime endTime, String message, int guestId);

    // TODO: Change to getBookings(int itemId) -> this will return bookings not
    // cancelled, declined, finished for the current version of that item
    List<BookingOrm> getBookingsForVersion(int versionId);

    BookingSearchResult searchBookings(int userId, boolean asHost,
            String searchQuery, String rawDate, String rawStatus,
            Integer page, Integer pageSize, String sortBy);

    void acceptBooking(int bookingId, int callerId);

    void rejectBooking(int bookingId, int callerId);

    void submitPayment(int bookingId, PaymentProofOrm payment, int callerId);

    Optional<PaymentProofOrm> getPaymentProofForParticipant(int bookingId, int callerId);

    void confirmPayment(int bookingId, int callerId);

    void rejectPayment(int bookingId, int callerId, String reason);

    void cancelBooking(int bookingId, int callerId);

    // Owner availability management

    Optional<OwnerAvailabilityPage> loadOwnerAvailabilityPage(int itemId, int ownerId, String requestedDate);

    BlockSlotOutcome blockSlotForOwner(int itemId, int ownerId, String date, String startTime, String endTime);

    boolean removeOwnerSelfBlock(int bookingId, int ownerId);

    enum BlockSlotOutcome {
        BLOCKED,
        PAST_DATE,
        OVERLAP,
        INVALID
    }

    // cron job
    void bookingResolutionRoutine();
}
