package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.MissingUserNamesException;
import ar.edu.itba.paw.services.SelfBookingNotAllowedException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class MarketplaceBookingRequestSubmitHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceBookingRequestSubmitHelper.class);

    private final BookingRequestService bookingRequestService;

    public enum Outcome {
        SUCCESS,
        SELF_BOOKING_NOT_ALLOWED,
        MISSING_USER_NAMES,
        UNEXPECTED_ERROR
    }

    public Outcome createBookingRequest(
            final Integer itemId,
            final String requesterGivenName,
            final String requesterLastName,
            final String requesterEmail,
            final String requesterPreferredLanguage,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String description) {
        try {
            bookingRequestService.createBookingRequest(
                    itemId,
                    requesterGivenName,
                    requesterLastName,
                    requesterEmail,
                    requesterPreferredLanguage,
                    startTime,
                    endTime,
                    description);
            return Outcome.SUCCESS;
        } catch (final SelfBookingNotAllowedException e) {
            return Outcome.SELF_BOOKING_NOT_ALLOWED;
        } catch (final MissingUserNamesException e) {
            return Outcome.MISSING_USER_NAMES;
        } catch (final Exception e) {
            LOGGER.error("Failed to create booking request for item {}.", itemId, e);
            return Outcome.UNEXPECTED_ERROR;
        }
    }
}
