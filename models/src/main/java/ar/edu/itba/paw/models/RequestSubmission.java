package ar.edu.itba.paw.models;

import java.time.Instant;

public class RequestSubmission {

    private final String token;
    private final String requesterName;
    private final String requesterEmail;
    private final String description;
    private RequestStatus status;
    private final Instant createdAt;
    private Instant resolvedAt;

    public RequestSubmission(
            final String token,
            final String requesterName,
            final String requesterEmail,
            final String description,
            final RequestStatus status,
            final Instant createdAt) {
        this.token = token;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve(final RequestStatus newStatus, final Instant resolutionTime) {
        this.status = newStatus;
        this.resolvedAt = resolutionTime;
    }
}
