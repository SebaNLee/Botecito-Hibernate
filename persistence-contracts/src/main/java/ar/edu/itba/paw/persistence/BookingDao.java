package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.BookingStatusEnumOrm;
import ar.edu.itba.paw.models.entity.PaymentProofOrm;
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
            BookingStatusEnumOrm status,
            String msg);

    List<BookingOrm> getBookingsForVersion(int versionId);

    Optional<BookingOrm> findById(int bookingId);

    Optional<Integer> findOwnerIdForBookingId(int bookingId);

    BookingSearchResult searchBookings(
            int userId,
            boolean asHost,
            String searchQuery,
            LocalDate date,
            BookingStatusEnumOrm status,
            Integer page,
            Integer pageSize,
            String sortBy);

    Optional<BookingOrm> updateStatusIncoming(int id, int callerId, BookingStatusEnumOrm status);

    Optional<BookingOrm> updateStatusOutgoing(int id, int callerId, BookingStatusEnumOrm status);

    Optional<PaymentProofOrm> uploadPayment(PaymentProofOrm paymentProof);

    Optional<PaymentProofOrm> findPaymentProofForParticipant(int bookingId, int userId);

    Optional<BookingOrm> refusePayment(int bookingId, String message, LocalDateTime refuseTime);

    void finalizeBookingsBefore(LocalDateTime maxEndTime);

    void expireBookingsBefore(LocalDateTime minStartTime);

    boolean startsAfter(int bookingId, LocalDateTime requestedStart);

    Optional<String> findVersionTimezone(int versionId);

    Optional<Integer> findOwnerIdForVersion(int versionId);

    List<AvailabilityOrm> listAvailabilitiesForVersion(int versionId);

    boolean deleteOwnerSelfBlock(int bookingId, int ownerId);
}
