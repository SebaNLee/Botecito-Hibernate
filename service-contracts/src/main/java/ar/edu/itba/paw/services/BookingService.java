package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.dto.SelfBlockCreate;
import ar.edu.itba.paw.models.dto.SelfBlockUpdate;
import ar.edu.itba.paw.models.dto.SelfBookingData;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingService {

    void createBooking(int itemId, LocalDate date, LocalTime startTime, LocalTime endTime, String message, int guestId);

    SearchResult<Booking> searchBookings(
            int userId,
            boolean asHost,
            String searchQuery,
            String rawDate,
            String rawStatus,
            Integer page,
            Integer pageSize,
            String sortBy);

    List<Booking> getUpcomingBookings(Item item);

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

    SelfBookingData getSelfBlocks(int itemId, int ownerId, LocalDate requestedDate);

    void saveSelfBlockChanges(
            int itemId,
            int ownerId,
            LocalDate date,
            List<Integer> deletedBlockIds,
            List<SelfBlockUpdate> updates,
            List<SelfBlockCreate> creates);

    // cron job
    void bookingResolutionRoutine();
}
