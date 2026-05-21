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

    // TODO: Revisar si realmente hace falta esto, capaz puede ser atributo de Version?
    List<Booking> getBookingsForVersion(int versionId);

    Optional<Booking> findById(int bookingId);

    // Redundante, se puede buscar el booking y leer version.item.host
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

    // Single update, pasa a service
    Optional<Booking> updateStatusIncoming(int id, int callerId, BookingStatusEnum status);

    // Idem ^
    Optional<Booking> updateStatusOutgoing(int id, int callerId, BookingStatusEnum status);

    // Upsert es raro, va a pasar a ser insert. Para updates, hacer un get del booking
    Optional<PaymentProof> uploadPayment(PaymentProof paymentProof);

    Optional<PaymentProof> findPaymentProofForParticipant(int bookingId, int userId);

    // Single update
    Optional<Booking> refusePayment(int bookingId, String message, LocalDateTime refuseTime);

    void finalizeBookingsBefore(LocalDateTime maxEndTime);

    void expireBookingsBefore(LocalDateTime minStartTime);

    // Single entity check, pasa a service
    boolean startsAfter(int bookingId, LocalDateTime requestedStart);

    // TODO: mover estos 3, no le corresponden a bookings
    // -------------
    Optional<String> findVersionTimezone(int versionId);

    Optional<Integer> findOwnerIdForVersion(int versionId);

    List<Availability> listAvailabilitiesForVersion(int versionId);
    // -------------

    // Owner check pasa a service
    boolean deleteOwnerSelfBlock(int bookingId, int ownerId);
}
