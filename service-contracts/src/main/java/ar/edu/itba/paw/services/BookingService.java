package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.dto.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingService {

    int createBooking(
            int versionId, LocalDate date, LocalTime startTime, LocalTime endTime, String message, int guestId);

    // TODO: Change to getBookings(int itemId) -> this will return bookings not
    // cancelled, declined, finished for the current version of that item
    List<Booking> getBookingsForVersion(int versionId);

    BookingSearchResult searchBookings(
            int userId,
            boolean asHost,
            String searchQuery,
            String rawDate,
            String rawStatus,
            Integer page,
            Integer pageSize,
            String sortBy);

    void acceptBooking(int bookingId, int callerId);

    void rejectBooking(int bookingId, int callerId);

    void submitPayment(int bookingId, PaymentProof payment, int callerId);

    Optional<PaymentProof> getPaymentProofForParticipant(int bookingId, int callerId);

    void confirmPayment(int bookingId, int callerId);

    void rejectPayment(int bookingId, int callerId, String reason);

    void cancelBooking(int bookingId, int callerId);

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
