package ar.edu.itba.paw.models;

import java.time.Instant;
import java.util.Locale;

public class BookingRequest {

    private final String token;
    private final Integer itemId;
    private final String requesterName;
    private final String requesterEmail;
    private final String requesterLocaleTag;
    private final String description;
    private BookingRequestStatus status;
    private final Instant createdAt;
    private Instant resolvedAt;

    public BookingRequest(
            final String token,
            final Integer itemId,
            final String requesterName,
            final String requesterEmail,
            final String requesterLocaleTag,
            final String description,
            final BookingRequestStatus status,
            final Instant createdAt) {
        this.token = token;
        this.itemId = itemId;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requesterLocaleTag = requesterLocaleTag;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getRequesterLocaleTag() {
        return requesterLocaleTag;
    }

    public Locale getRequesterLocale() {
        return Locale.forLanguageTag(requesterLocaleTag);
    }

    public String getDescription() {
        return description;
    }

    public BookingRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve(final BookingRequestStatus newStatus, final Instant resolutionTime) {
        this.status = newStatus;
        this.resolvedAt = resolutionTime;
    }
}
