package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingRequestStatus;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface BookingRequestService {
    BookingRequest createBookingRequest(
            Integer itemId,
            String requesterName,
            String requesterEmail,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String description);

    Optional<BookingRequest> findByToken(String token);

    Optional<BookingRequest> resolveBookingRequest(String token, BookingRequestStatus newStatus);
}
