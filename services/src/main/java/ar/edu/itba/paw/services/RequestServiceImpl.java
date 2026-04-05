package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RequestServiceImpl implements RequestService {

    private final Map<String, RequestSubmission> requestsByToken = new ConcurrentHashMap<>();
    private final MailService mailService;

    public RequestServiceImpl(final MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public RequestSubmission createRequest(
            final String requesterName, final String requesterEmail, final String description) {
        final String token = UUID.randomUUID().toString();
        final RequestSubmission requestSubmission = new RequestSubmission(
                token,
                requesterName,
                requesterEmail,
                mailService.resolveLocale(requesterEmail).toLanguageTag(),
                description,
                RequestStatus.PENDING,
                Instant.now());

        requestsByToken.put(token, requestSubmission);

        // TODO replace in-memory storage with persistence once the request table is available.
        return requestSubmission;
    }

    @Override
    public Optional<RequestSubmission> findByToken(final String token) {
        return Optional.ofNullable(requestsByToken.get(token));
    }

    @Override
    public Optional<RequestSubmission> resolveRequest(final String token, final RequestStatus newStatus) {
        final RequestSubmission requestSubmission = requestsByToken.get(token);
        if (requestSubmission == null || requestSubmission.getStatus() != RequestStatus.PENDING) {
            return Optional.empty();
        }

        requestSubmission.resolve(newStatus, Instant.now());
        // TODO persist the new status and resolution timestamp once the DB is ready.
        return Optional.of(requestSubmission);
    }
}
