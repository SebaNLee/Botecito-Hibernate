package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingRequestServiceImpl implements BookingRequestService {

    private static final int MIN_ANTICIPATION_MINUTES = 120;

    private final ItemDao itemDao;
    private final MailService mailService;

    public BookingRequestServiceImpl(final ItemDao itemDao, final MailService mailService) {
        this.itemDao = itemDao;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public BookingRequest createBookingRequest(
            final Integer itemId,
            final String requesterGivenName,
            final String requesterLastName,
            final String requesterEmail,
            final String requesterPreferredLanguage,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String description) {
        validateAnticipation(startTime);
        final User requesterUser = resolveOrCreateRequesterUser(
                requesterGivenName, requesterLastName, requesterEmail, requesterPreferredLanguage);
        final String token = UUID.randomUUID().toString();
        final ItemBooking booking =
                itemDao.createBookingRequest(itemId, requesterUser.getId(), startTime, endTime, description, token);
        return toBookingRequest(booking, requesterUser);
    }

    private static OffsetDateTime currentDateTime() {
        // UTC-3 (otras formas de hacer esto no funcionaron)
        return OffsetDateTime.now().minusHours(3);
    }

    private static void validateAnticipation(final OffsetDateTime bookingStartTime) {
        final Instant now = currentDateTime().toInstant();
        final Instant bookingStart = bookingStartTime.toInstant();
        final Instant earliestAllowedStart = now.plus(Duration.ofMinutes(MIN_ANTICIPATION_MINUTES));
        if (bookingStart.isBefore(earliestAllowedStart)) {
            throw new RuntimeException("Requested booking starts in less than the minimum anticipation time of "
                    + MIN_ANTICIPATION_MINUTES + " minutes");
        }
    }

    private void validateAnticipationByToken(final String hostDecisionToken) {
        itemDao.findBookingByHostDecisionToken(hostDecisionToken)
                .map(ItemBooking::getStartTime)
                .ifPresent(BookingRequestServiceImpl::validateAnticipation);
    }

    @Override
    public Optional<BookingRequest> findByToken(final String token) {
        return itemDao.findBookingByHostDecisionToken(token).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> resolveBookingRequest(final String token, final BookingState newStatus) {
        if (newStatus == BookingState.BOOKING_CONFIRMED) {
            validateAnticipationByToken(token);
        }
        if (!itemDao.resolveBookingByHostDecisionToken(token, newStatus, currentDateTime())) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    @Override
    @Transactional
    public void expireAllDue(final OffsetDateTime currentDateTime) {
        itemDao.expireAllDueBookings(currentDateTime.plusMinutes(MIN_ANTICIPATION_MINUTES));
    }

    // @Scheduled(cron = "0 0,30 * * * *")
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireDueBookingsSchedule() {
        expireAllDue(currentDateTime());
    }

    @Override
    public Optional<BookingPaymentProof> submitPaymentProof(
            final int bookingId,
            final int requesterId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply) {
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()
                || booking.get().getGuestId() == null
                || booking.get().getGuestId() != requesterId
                || !canSubmitPaymentProof(booking.get().getState())) {
            return Optional.empty();
        }

        validateAnticipation(booking.get().getStartTime());

        final BookingState state = booking.get().getState();
        if (state == BookingState.BOOKING_CONFIRMED) {
            if (itemDao.findPaymentProofByBookingId(bookingId).isPresent()) {
                return Optional.empty();
            }
            if (!itemDao.markBookingPaymentSubmitted(bookingId, requesterId)) {
                return Optional.empty();
            }
        } else if (state == BookingState.BOOKING_PAYMENT_REFUSED) {
            itemDao.deletePaymentProofByBookingId(bookingId);
            if (!itemDao.markBookingPaymentResubmitted(bookingId, requesterId)) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
        return Optional.of(itemDao.createPaymentProof(
                bookingId, requesterId, fileName, contentType, fileData, normalizeReply(guestReply)));
    }

    @Override
    public Optional<BookingRequest> refusePaymentProof(final int bookingId, final int ownerId, final String reason) {
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()
                || booking.get().getItemId() == null
                || booking.get().getState() != BookingState.BOOKING_PAYMENT_SUBMITTED
                || itemDao.findPaymentProofByBookingId(bookingId).isEmpty()) {
            return Optional.empty();
        }

        final Optional<Item> item = itemDao.findItemById(booking.get().getItemId());
        if (item.isEmpty()
                || item.get().getOwnerId() == null
                || !item.get().getOwnerId().equals(ownerId)) {
            return Optional.empty();
        }

        final String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        validateAnticipation(booking.get().getStartTime());

        if (!itemDao.markBookingPaymentRefused(bookingId, ownerId, trimmed)) {
            return Optional.empty();
        }
        return itemDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> confirmPaymentReceived(final int bookingId, final int ownerId) {
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()
                || booking.get().getItemId() == null
                || booking.get().getHostDecisionToken() == null
                || booking.get().getState() != BookingState.BOOKING_PAYMENT_SUBMITTED
                || itemDao.findPaymentProofByBookingId(bookingId).isEmpty()) {
            return Optional.empty();
        }

        final Optional<Item> item = itemDao.findAnyItemById(booking.get().getItemId());
        if (item.isEmpty()
                || item.get().getOwnerId() == null
                || !item.get().getOwnerId().equals(ownerId)) {
            return Optional.empty();
        }

        if (!itemDao.markBookingPaid(bookingId, ownerId)) {
            return Optional.empty();
        }
        return itemDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId) {
        return itemDao.findPaymentProofByBookingId(bookingId);
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

    private static boolean canSubmitPaymentProof(final BookingState state) {
        return state == BookingState.BOOKING_CONFIRMED || state == BookingState.BOOKING_PAYMENT_REFUSED;
    }

    private static String normalizeReply(final String reply) {
        if (reply == null) {
            return null;
        }
        final String trimmed = reply.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizePreferredLanguage(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return "en";
        }
        return "es";
    }
}
