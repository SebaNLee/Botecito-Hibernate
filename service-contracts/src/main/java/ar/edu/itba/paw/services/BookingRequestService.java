package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
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
}
