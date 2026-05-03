package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.services.dto.GuestTripsView;
import ar.edu.itba.paw.services.dto.PaymentProofUpload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRequestService {
    BookingRequest createBookingRequest(
            Integer itemId,
            String requesterGivenName,
            String requesterLastName,
            String requesterEmail,
            String requesterPreferredLanguage,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String description);

    Optional<BookingRequest> findByToken(String token);

    Optional<BookingRequest> resolveBookingRequest(String token, BookingState newStatus);

    void expireAllDue(OffsetDateTime currentDateTime);

    List<BookingRequest> resolveBookingRequests(List<String> tokens, BookingState newStatus);

    Optional<BookingPaymentProof> submitPaymentProof(
            int bookingId, int requesterId, String fileName, String contentType, byte[] fileData, String guestReply);

    Optional<BookingRequest> confirmPaymentReceived(int bookingId, int ownerId);

    Optional<BookingRequest> refusePaymentProof(int bookingId, int ownerId, String reason);

    Optional<BookingPaymentProof> findPaymentProofByBookingId(int bookingId);

    ItemBooking createOwnerSelfBlock(int itemId, int ownerId, OffsetDateTime startTime, OffsetDateTime endTime);

    boolean removeOwnerSelfBlock(int bookingId, int ownerId);

    // ---- View / orchestration extensions ----------------------------------------------------

    GuestTripsView buildGuestTrips(
            int guestUserId, List<String> statusFilters, String boatNameQuery, int page, int pageSize);

    /**
     * Stores a payment proof after running file-type validation (size + magic-byte sniff).
     * Returns empty when the booking does not exist, the requester is not the booking guest,
     * or the file fails validation.
     */
    Optional<BookingPaymentProof> submitPaymentProof(int bookingId, int requesterId, PaymentProofUpload upload);

    /** Domain rule: only the booking's guest or the item's owner may view the proof. */
    boolean canAccessPaymentProof(int bookingId, int viewerUserId);
}
