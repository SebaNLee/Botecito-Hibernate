package ar.edu.itba.paw.models;

import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BookingRequest {

    private final String token;
    private final Integer itemId;
    private final String requesterName;
    private final String requesterEmail;
    private final String requesterLocaleTag;
    private final String description;
    private BookingState status;
    private final Instant createdAt;
    private Instant resolvedAt;

    public BookingRequest(
            final String token,
            final Integer itemId,
            final String requesterName,
            final String requesterEmail,
            final String requesterLocaleTag,
            final String description,
            final BookingState status,
            final Instant createdAt) {
        this(token, itemId, requesterName, requesterEmail, requesterLocaleTag, description, status, createdAt, null);
    }

    public Locale getRequesterLocale() {
        return Locale.forLanguageTag(requesterLocaleTag);
    }

    public void resolve(final BookingState newStatus, final Instant resolutionTime) {
        this.status = newStatus;
        this.resolvedAt = resolutionTime;
    }
}
