package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface RequestService {
    RequestSubmission createRequest(
            Integer itemId,
            String requesterName,
            String requesterEmail,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String description);

    Optional<RequestSubmission> findByToken(String token);

    Optional<RequestSubmission> resolveRequest(String token, RequestStatus newStatus);
}
