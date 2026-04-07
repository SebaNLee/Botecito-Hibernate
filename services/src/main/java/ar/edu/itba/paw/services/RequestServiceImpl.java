package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RequestServiceImpl implements RequestService {

    private final ItemDao itemDao;
    private final MailService mailService;

    public RequestServiceImpl(final ItemDao itemDao, final MailService mailService) {
        this.itemDao = itemDao;
        this.mailService = mailService;
    }

    @Override
    public RequestSubmission createRequest(
            final Integer itemId,
            final String requesterName,
            final String requesterEmail,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String description) {
        final User requesterUser = resolveOrCreateRequesterUser(requesterName, requesterEmail);
        final String token = UUID.randomUUID().toString();
        final ItemBooking booking =
                itemDao.createBookingRequest(itemId, requesterUser.getId(), startTime, endTime, description, token);
        return toRequestSubmission(booking, requesterUser);
    }

    @Override
    public Optional<RequestSubmission> findByToken(final String token) {
        return itemDao.findBookingByHostDecisionToken(token).flatMap(this::toRequestSubmission);
    }

    @Override
    public Optional<RequestSubmission> resolveRequest(final String token, final RequestStatus newStatus) {
        if (!itemDao.resolveBookingByHostDecisionToken(token, toBookingState(newStatus), OffsetDateTime.now())) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    private User resolveOrCreateRequesterUser(final String requesterName, final String requesterEmail) {
        return itemDao.findUserByEmail(requesterEmail).orElseGet(() -> {
            final String trimmedName = requesterName == null ? "" : requesterName.trim();
            if (trimmedName.isEmpty()) {
                return itemDao.createUser("Guest", "", requesterEmail);
            }
            final int separatorIndex = trimmedName.indexOf(' ');
            if (separatorIndex < 0) {
                return itemDao.createUser(trimmedName, "", requesterEmail);
            }
            final String givenName = trimmedName.substring(0, separatorIndex);
            final String lastName = trimmedName.substring(separatorIndex + 1).trim();
            return itemDao.createUser(givenName, lastName, requesterEmail);
        });
    }

    private Optional<RequestSubmission> toRequestSubmission(final ItemBooking booking) {
        return itemDao.findUserById(booking.getGuestId()).map(user -> toRequestSubmission(booking, user));
    }

    private RequestSubmission toRequestSubmission(final ItemBooking booking, final User requesterUser) {
        final RequestSubmission requestSubmission = new RequestSubmission(
                booking.getHostDecisionToken(),
                booking.getItemId(),
                requesterUser.getName(),
                requesterUser.getEmail(),
                resolveRequesterLocaleTag(requesterUser),
                booking.getRequestMessage(),
                toRequestStatus(booking.getState()),
                booking.getCreatedAt() == null
                        ? Instant.now()
                        : booking.getCreatedAt().toInstant());

        if (booking.getHostDecisionUsedAt() != null) {
            requestSubmission.resolve(
                    toRequestStatus(booking.getState()),
                    booking.getHostDecisionUsedAt().toInstant());
        }
        return requestSubmission;
    }

    private String resolveRequesterLocaleTag(final User requesterUser) {
        if (requesterUser.getPreferredLanguage() != null
                && !requesterUser.getPreferredLanguage().isBlank()) {
            return requesterUser.getPreferredLanguage();
        }
        return mailService.resolveLocale(requesterUser.getEmail()).toLanguageTag();
    }

    private static BookingState toBookingState(final RequestStatus requestStatus) {
        return switch (requestStatus) {
            case ACCEPTED -> BookingState.BOOKING_CONFIRMED;
            case DECLINED -> BookingState.BOOKING_REJECTED;
            default -> BookingState.BOOKING_PENDING;
        };
    }

    private static RequestStatus toRequestStatus(final BookingState bookingState) {
        return switch (bookingState) {
            case BOOKING_CONFIRMED -> RequestStatus.ACCEPTED;
            case BOOKING_REJECTED -> RequestStatus.DECLINED;
            default -> RequestStatus.PENDING;
        };
    }
}
