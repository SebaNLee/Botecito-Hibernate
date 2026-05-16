package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingDao {

    Optional<Integer> insertBooking(
            int versionId,
            int guestId,
            LocalDateTime utcStart,
            LocalDateTime utcEnd,
            BookingStatusEnum status,
            String msg);

    List<Booking> getBookingsForVersion(int versionId);

    Optional<Booking> findById(int bookingId);

    Optional<Integer> findOwnerIdForBookingId(int bookingId);

    BookingSearchResult searchBookings(
            int userId,
            boolean asHost,
            String searchQuery,
            LocalDate date,
            BookingStatusEnum status,
            Integer page,
            Integer pageSize,
            String sortBy);

    Optional<Booking> updateStatusIncoming(int id, int callerId, BookingStatusEnum status);

    Optional<Booking> updateStatusOutgoing(int id, int callerId, BookingStatusEnum status);

    Optional<PaymentProof> uploadPayment(PaymentProof paymentProof);

    Optional<PaymentProof> findPaymentProofForParticipant(int bookingId, int userId);

    Optional<Booking> refusePayment(int bookingId, String message, LocalDateTime refuseTime);

    void finalizeBookingsBefore(LocalDateTime maxEndTime);

    void expireBookingsBefore(LocalDateTime minStartTime);

    boolean startsAfter(int bookingId, LocalDateTime requestedStart);

    Optional<String> findVersionTimezone(int versionId);

    Optional<Integer> findOwnerIdForVersion(int versionId);

    List<Availability> listAvailabilitiesForVersion(int versionId);

    boolean deleteOwnerSelfBlock(int bookingId, int ownerId);
}
