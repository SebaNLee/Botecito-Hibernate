package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.dto.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface BookingService {

    void createBooking(int itemId, LocalDate date, LocalTime startTime, LocalTime endTime, String message, int guestId);

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

    // Inserta si no hay un pago cargado, si no actualiza
    void submitPayment(
            final int bookingId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestMsg,
            final int callerId);

    Optional<PaymentProof> getPaymentProofForParticipant(int bookingId, int callerId);

    void confirmPayment(int bookingId, int callerId);

    void rejectPayment(int bookingId, int callerId, String reason);

    void cancelBooking(int bookingId, int callerId);

    OwnerAvailabilityPage loadOwnerAvailabilityPage(int itemId, int ownerId, String requestedDate);

    void blockSlotForOwner(int itemId, int ownerId, String date, String startTime, String endTime);

    boolean removeOwnerSelfBlock(int bookingId, int ownerId);

    // cron job
    void bookingResolutionRoutine();
}
