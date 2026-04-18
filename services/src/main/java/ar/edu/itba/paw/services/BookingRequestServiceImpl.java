package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookingRequestServiceImpl implements BookingRequestService {
    private static final long MIN_BOOKING_MINUTES = 120;
    private static final int TIME_STEP_MINUTES = 30;

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
        final int validatedItemId = validateBookableItem(itemId);
        final String normalizedRequesterGivenName = ServiceInputValidator.optionalText(
                requesterGivenName, "requester given name", ServiceInputValidator.NAME_MAX_LENGTH);
        final String normalizedRequesterLastName = ServiceInputValidator.optionalText(
                requesterLastName, "requester last name", ServiceInputValidator.NAME_MAX_LENGTH);
        final String normalizedRequesterEmail = ServiceInputValidator.requireEmail(requesterEmail);
        final String normalizedDescription = ServiceInputValidator.optionalText(
                description, "description", ServiceInputValidator.DESCRIPTION_MAX_LENGTH);
        validateBookingTime(validatedItemId, startTime, endTime);

        final User requesterUser = resolveOrCreateRequesterUser(
                normalizedRequesterGivenName,
                normalizedRequesterLastName,
                normalizedRequesterEmail,
                requesterPreferredLanguage);
        final String token = UUID.randomUUID().toString();
        final ItemBooking booking = itemDao.createBookingRequest(
                validatedItemId, requesterUser.getId(), startTime, endTime, normalizedDescription, token);
        return toBookingRequest(booking, requesterUser);
    }

    @Override
    public Optional<BookingRequest> findByToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return itemDao.findBookingByHostDecisionToken(token).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> resolveBookingRequest(final String token, final BookingState newStatus) {
        if (token == null || token.isBlank() || !isAllowedResolutionState(newStatus)) {
            return Optional.empty();
        }
        if (!itemDao.resolveBookingByHostDecisionToken(token, newStatus, OffsetDateTime.now())) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    private int validateBookableItem(final Integer itemId) {
        final int validatedItemId = ServiceInputValidator.requirePositive(itemId, "item id");
        final Item item = itemDao.findItemById(validatedItemId)
                .orElseThrow(() -> new IllegalArgumentException("item does not exist"));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("item is not active");
        }
        return validatedItemId;
    }

    private void validateBookingTime(final int itemId, final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("booking start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("booking end time must be after start time");
        }
        if (!startTime.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("booking start time must be in the future");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new IllegalArgumentException("booking must start and end on the same date");
        }
        if (!isThirtyMinuteStep(startTime) || !isThirtyMinuteStep(endTime)) {
            throw new IllegalArgumentException("booking times must use 30 minute steps");
        }
        if (Duration.between(startTime, endTime).toMinutes() < MIN_BOOKING_MINUTES) {
            throw new IllegalArgumentException("booking must be at least two hours long");
        }
        if (!isWithinPublishedAvailability(itemId, startTime, endTime)) {
            throw new IllegalArgumentException("booking is outside item availability");
        }
        if (overlapsBlockingBooking(itemId, startTime, endTime)) {
            throw new IllegalArgumentException("booking overlaps an existing booking");
        }
    }

    private boolean isWithinPublishedAvailability(
            final int itemId, final OffsetDateTime startTime, final OffsetDateTime endTime) {
        final List<ItemAvailability> availabilities = itemDao.listAvailabilitiesByItemId(itemId);
        for (final ItemAvailability availability : availabilities) {
            if (availability.getWeekday() == startTime.getDayOfWeek()
                    && !startTime.toLocalTime().isBefore(availability.getStartTime())
                    && !endTime.toLocalTime().isAfter(availability.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsBlockingBooking(
            final int itemId, final OffsetDateTime startTime, final OffsetDateTime endTime) {
        for (final ItemBooking booking : itemDao.listBookingsByItemId(itemId)) {
            if (!isBlockingBooking(booking)) {
                continue;
            }
            if (booking.getStartTime() == null || booking.getEndTime() == null) {
                continue;
            }
            if (startTime.isBefore(booking.getEndTime()) && endTime.isAfter(booking.getStartTime())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == null
                || booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED;
    }

    private static boolean isAllowedResolutionState(final BookingState newStatus) {
        return newStatus == BookingState.BOOKING_CONFIRMED || newStatus == BookingState.BOOKING_REJECTED;
    }

    private static boolean isThirtyMinuteStep(final OffsetDateTime time) {
        return time.getMinute() % TIME_STEP_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
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
