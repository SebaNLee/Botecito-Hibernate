package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import java.util.Optional;

public interface RequestService {
    RequestSubmission createRequest(String requesterName, String requesterEmail, String description);

    Optional<RequestSubmission> findByToken(String token);

    Optional<RequestSubmission> resolveRequest(String token, RequestStatus newStatus);
}
