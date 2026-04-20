package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookingRequestServiceImpl implements BookingRequestService {
    private final ItemDao itemDao;
    private final MailService mailService;

    public BookingRequestServiceImpl(final ItemDao itemDao, final MailService mailService) {
        this.itemDao = itemDao;
        this.mailService = mailService;
    }

    @Override
    public BookingRequest createBookingRequest(
            final Integer itemId,
            final String requesterGivenName,
            final String requesterLastName,
            final String requesterEmail,
            final String requesterPreferredLanguage,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String description) {
        final User requesterUser = resolveOrCreateRequesterUser(
                requesterGivenName, requesterLastName, requesterEmail, requesterPreferredLanguage);
        final String token = UUID.randomUUID().toString();
        final ItemBooking booking =
                itemDao.createBookingRequest(itemId, requesterUser.getId(), startTime, endTime, description, token);
        return toBookingRequest(booking, requesterUser);
    }

    @Override
    public Optional<BookingRequest> findByToken(final String token) {
        return itemDao.findBookingByHostDecisionToken(token).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> resolveBookingRequest(final String token, final BookingState newStatus) {
        if (!itemDao.resolveBookingByHostDecisionToken(token, newStatus, OffsetDateTime.now())) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    private User resolveOrCreateRequesterUser(
            final String requesterGivenName,
            final String requesterLastName,
            final String requesterEmail,
            final String requesterPreferredLanguage) {
        final String givenName = normalizeNamePart(requesterGivenName, "Guest");
        final String lastName = normalizeNamePart(requesterLastName, "");
        final String preferredLanguage = normalizePreferredLanguage(requesterPreferredLanguage);

        final Optional<User> existingUser = itemDao.findUserByEmail(requesterEmail);
        if (existingUser.isPresent()) {
            final User user = existingUser.get();
            itemDao.updateUserProfile(user.getId(), givenName, lastName, preferredLanguage);
            user.setGivenName(givenName);
            user.setLastName(lastName);
            user.setPreferredLanguage(preferredLanguage);
            return user;
        }

        return itemDao.createUser(givenName, lastName, requesterEmail, preferredLanguage);
    }

    private Optional<BookingRequest> toBookingRequest(final ItemBooking booking) {
        return itemDao.findUserById(booking.getGuestId()).map(user -> toBookingRequest(booking, user));
    }

    private BookingRequest toBookingRequest(final ItemBooking booking, final User requesterUser) {
        final BookingRequest bookingRequest = new BookingRequest(
                booking.getHostDecisionToken(),
                booking.getItemId(),
                requesterUser.getName(),
                requesterUser.getEmail(),
                resolveRequesterLocaleTag(requesterUser),
                booking.getRequestMessage(),
                booking.getState(),
                booking.getCreatedAt() == null
                        ? Instant.now()
                        : booking.getCreatedAt().toInstant());

        if (booking.getHostDecisionUsedAt() != null) {
            bookingRequest.resolve(
                    booking.getState(), booking.getHostDecisionUsedAt().toInstant());
        }
        return bookingRequest;
    }

    private String resolveRequesterLocaleTag(final User requesterUser) {
        if (requesterUser.getPreferredLanguage() != null
                && !requesterUser.getPreferredLanguage().isBlank()) {
            return requesterUser.getPreferredLanguage();
        }
        return mailService.resolveLocale(requesterUser.getEmail()).toLanguageTag();
    }

    private static String normalizeNamePart(final String value, final String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String normalizePreferredLanguage(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return "en";
        }
        return "es";
    }
}
