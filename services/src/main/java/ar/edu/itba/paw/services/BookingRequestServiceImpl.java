package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.services.utils.PaymentProofValidator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingRequestServiceImpl implements BookingRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookingRequestServiceImpl.class);

    private static final int MIN_ANTICIPATION_MINUTES = 120;
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final ItemDao itemDao;
    private final MailService mailService;

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
        final Item item = itemDao.findAnyItemById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (item.getOwnerId() != null && item.getOwnerId().equals(requesterUser.getId())) {
            throw new SelfBookingNotAllowedException();
        }
        final String token = UUID.randomUUID().toString();
        final ItemBooking booking =
                itemDao.createBookingRequest(itemId, requesterUser.getId(), startTime, endTime, description, token);
        LOGGER.info(
                "Booking request created for item {} by user {} done at {} for startTime: {} and endTime: {}",
                itemId,
                requesterUser.getId(),
                currentDateTime().toString(),
                startTime.toString(),
                endTime.toString());
        final BookingRequest bookingRequest = toBookingRequest(booking, requesterUser);
        sendBookingReviewEmail(bookingRequest, item, startTime, endTime);
        return bookingRequest;
    }

    private static OffsetDateTime currentDateTime() {
        return OffsetDateTime.now();
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
    @Transactional
    public ItemBooking createOwnerSelfBlock(
            final int itemId, final int ownerId, final OffsetDateTime startTime, final OffsetDateTime endTime) {
        final Item item = itemDao.findItemByIdForOwner(itemId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found or not owned by user"));
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Personal block start must be before end");
        }
        for (final ItemBooking booking : itemDao.listBookingsByItemId(item.getId())) {
            if (isBlockingBooking(booking)
                    && rangesOverlap(startTime, endTime, booking.getStartTime(), booking.getEndTime())) {
                throw new OverlappingActiveBookingException();
            }
        }
        final OffsetDateTime recordedAt = OffsetDateTime.now();
        return itemDao.insertOwnerPersonalBlock(
                itemId, ownerId, startTime, endTime, UUID.randomUUID().toString(), recordedAt);
    }

    @Override
    @Transactional
    public boolean removeOwnerSelfBlock(final int bookingId, final int ownerId) {
        final Optional<ItemBooking> bookingOpt = itemDao.findBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            return false;
        }
        final ItemBooking booking = bookingOpt.get();
        if (booking.getItemId() == null
                || booking.getGuestId() == null
                || !Objects.equals(booking.getGuestId(), ownerId)
                || booking.getState() != BookingState.BOOKING_CONFIRMED) {
            return false;
        }
        final Item item = itemDao.findAnyItemById(booking.getItemId()).orElse(null);
        if (item == null
                || item.getOwnerId() == null
                || !Objects.equals(item.getOwnerId(), ownerId)
                || !Objects.equals(item.getOwnerId(), booking.getGuestId())) {
            return false;
        }
        return itemDao.markBookingCancelled(bookingId);
    }

    @Override
    public Optional<BookingRequest> findByToken(final String token) {
        return itemDao.findBookingByHostDecisionToken(token).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> resolveBookingRequest(final String token, final BookingState newStatus) {
        expireAllDue(currentDateTime());
        LOGGER.info("Resolving booking request to status {}", newStatus);
        if (newStatus == BookingState.BOOKING_CONFIRMED) {
            validateAnticipationByToken(token);
        }
        if (!itemDao.resolveBookingByHostDecisionToken(token, newStatus, currentDateTime())) {
            return Optional.empty();
        }
        final Optional<BookingRequest> resolved = findByToken(token);
        resolved.ifPresent(this::sendBookingResolutionEmail);
        return resolved;
    }

    @Override
    @Transactional
    public void expireAllDue(final OffsetDateTime currentDateTime) {
        itemDao.expireAllDueBookings(currentDateTime.plusMinutes(MIN_ANTICIPATION_MINUTES));
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireDueBookingsSchedule() {
        LOGGER.info("Running cron job! currentDateTime: {} ", currentDateTime().toString());
        expireAllDue(currentDateTime());
    }

    public List<BookingRequest> resolveBookingRequests(final List<String> tokens, final BookingState newStatus) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        final LinkedHashSet<String> normalizedTokens = new LinkedHashSet<>();
        for (final String token : tokens) {
            if (token != null && !token.isBlank()) {
                normalizedTokens.add(token);
            }
        }
        if (normalizedTokens.isEmpty()) {
            return List.of();
        }

        final List<ItemBooking> candidateBookings = itemDao.findBookingsByHostDecisionTokens(normalizedTokens);
        if (candidateBookings.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> pendingTokens = new LinkedHashSet<>();
        for (final ItemBooking booking : candidateBookings) {
            if (booking.getState() == BookingState.BOOKING_PENDING
                    && booking.getHostDecisionUsedAt() == null
                    && booking.getHostDecisionToken() != null
                    && !booking.getHostDecisionToken().isBlank()) {
                pendingTokens.add(booking.getHostDecisionToken());
            }
        }
        if (pendingTokens.isEmpty()) {
            return List.of();
        }

        final OffsetDateTime resolvedAt = OffsetDateTime.now();
        itemDao.resolveBookingsByHostDecisionTokens(pendingTokens, newStatus, resolvedAt);
        LOGGER.info("Resolved {} booking requests to status {}", pendingTokens.size(), newStatus);

        final List<ItemBooking> updatedBookings = itemDao.findBookingsByHostDecisionTokens(pendingTokens);
        if (updatedBookings.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<Integer> guestIds = new LinkedHashSet<>();
        for (final ItemBooking booking : updatedBookings) {
            if (booking.getState() == newStatus && booking.getGuestId() != null) {
                guestIds.add(booking.getGuestId());
            }
        }
        final Map<Integer, User> usersById = new LinkedHashMap<>();
        for (final User user : itemDao.findUsersByIds(guestIds)) {
            usersById.put(user.getId(), user);
        }

        final List<BookingRequest> resolvedRequests = new java.util.ArrayList<>();
        for (final ItemBooking booking : updatedBookings) {
            if (booking.getState() != newStatus || booking.getGuestId() == null) {
                continue;
            }
            final User requester = usersById.get(booking.getGuestId());
            if (requester == null) {
                continue;
            }
            final BookingRequest request = toBookingRequest(booking, requester);
            request.resolve(newStatus, resolvedAt.toInstant());
            resolvedRequests.add(request);
        }
        resolvedRequests.forEach(this::sendBookingResolutionEmail);
        return resolvedRequests;
    }

    @Override
    public Optional<BookingPaymentProof> submitPaymentProof(
            final int bookingId,
            final int requesterId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply) {
        if (!PaymentProofValidator.isValid(fileName, contentType, fileData)) {
            return Optional.empty();
        }
        expireAllDue(currentDateTime());
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()
                || booking.get().getGuestId() == null
                || booking.get().getGuestId() != requesterId
                || !canSubmitPaymentProof(booking.get().getState())) {
            LOGGER.warn(
                    "Attempt to submit payment proof for invalid booking {} by requester {}", bookingId, requesterId);
            return Optional.empty();
        }

        validateAnticipation(booking.get().getStartTime());

        final BookingState state = booking.get().getState();
        if (state == BookingState.BOOKING_CONFIRMED) {
            if (itemDao.findPaymentProofByBookingId(bookingId).isPresent()) {
                LOGGER.warn("Payment proof already exists for booking {}", bookingId);
                return Optional.empty();
            }
            if (!itemDao.markBookingPaymentSubmitted(bookingId, requesterId)) {
                LOGGER.error("Failed to mark booking {} as payment submitted", bookingId);
                return Optional.empty();
            }
        } else if (state == BookingState.BOOKING_PAYMENT_REFUSED) {
            itemDao.deletePaymentProofByBookingId(bookingId);
            if (!itemDao.markBookingPaymentResubmitted(bookingId, requesterId)) {
                LOGGER.error("Failed to mark booking {} as payment resubmitted", bookingId);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("Invalid state {} for payment submission on booking {}", state, bookingId);
            return Optional.empty();
        }

        LOGGER.info("Submitting payment proof for booking {}", bookingId);

        final BookingPaymentProof proof = itemDao.createPaymentProof(
                bookingId, requesterId, fileName, contentType, fileData, normalizeReply(guestReply));
        sendPaymentProofSubmittedEmail(booking.get(), requesterId, proof);
        return Optional.of(proof);
    }

    @Override
    public Optional<BookingRequest> refusePaymentProof(final int bookingId, final int ownerId, final String reason) {
        expireAllDue(currentDateTime());
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);

        LOGGER.debug("Refusing payment proof for booking {} by owner {}", bookingId, ownerId);

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
        final Optional<BookingRequest> refused =
                itemDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
        refused.ifPresent(request -> sendPaymentProofRefusedEmail(request, ownerId, trimmed));
        return refused;
    }

    @Override
    public Optional<BookingRequest> confirmPaymentReceived(final int bookingId, final int ownerId) {
        expireAllDue(currentDateTime());
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);

        LOGGER.debug("Confirming payment for booking {} by owner {}", bookingId, ownerId);

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
        final Optional<BookingRequest> paid = itemDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
        paid.ifPresent(this::sendPaymentReceivedEmail);
        return paid;
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
        ar.edu.itba.paw.services.utils.UserNameRules.requireBothLegalNames(requesterGivenName, requesterLastName);
        final String givenName = normalizeNamePart(requesterGivenName, "Guest");
        final String lastName = normalizeNamePart(requesterLastName, "");
        final String preferredLanguage = normalizePreferredLanguage(requesterPreferredLanguage);

        final Optional<User> existingUser = itemDao.findUserByEmail(requesterEmail);
        if (existingUser.isPresent()) {
            final User user = existingUser.get();
            itemDao.updateUserProfile(user.getId(), givenName, lastName, preferredLanguage);
            user.setGivenName(givenName);
            user.setLastName(lastName);
            user.setPreferredLanguage(ar.edu.itba.paw.models.PreferredLanguage.fromPersistence(preferredLanguage));
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
        if (requesterUser.getPreferredLanguage() != null) {
            return requesterUser.getPreferredLanguage().getPersistenceCode();
        }
        return mailService.resolveLocale(requesterUser.getEmail()).toLanguageTag();
    }

    private void sendBookingReviewEmail(
            final BookingRequest bookingRequest,
            final Item item,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime) {
        try {
            final String ownerEmail = item.getOwnerId() == null
                    ? null
                    : itemDao.findUserById(item.getOwnerId())
                            .map(User::getEmail)
                            .orElse(null);
            mailService.sendBookingReviewEmail(
                    bookingRequest,
                    ownerEmail,
                    item.getTitle(),
                    item.getLocation(),
                    startTime.toLocalDate().toString(),
                    startTime.toLocalTime().format(TIME_LABEL_FORMATTER) + " - "
                            + endTime.toLocalTime().format(TIME_LABEL_FORMATTER));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger booking review email for booking token {}.", bookingRequest.getToken(), e);
        }
    }

    private void sendBookingResolutionEmail(final BookingRequest bookingRequest) {
        try {
            mailService.sendBookingResolutionEmail(bookingRequest);
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not trigger booking resolution email for booking token {}.", bookingRequest.getToken(), e);
        }
    }

    private void sendPaymentProofSubmittedEmail(
            final ItemBooking booking, final int requesterId, final BookingPaymentProof proof) {
        try {
            if (booking.getItemId() == null) {
                return;
            }
            final Optional<Item> item = itemDao.findAnyItemById(booking.getItemId());
            if (item.isEmpty() || item.get().getOwnerId() == null) {
                return;
            }
            final Optional<User> owner = itemDao.findUserById(item.get().getOwnerId());
            if (owner.isEmpty()) {
                return;
            }
            final String requesterName =
                    itemDao.findUserById(requesterId).map(User::getName).orElse("");
            mailService.sendPaymentProofSubmittedEmail(
                    owner.get().getEmail(),
                    requesterName,
                    item.get().getTitle(),
                    proof.getFileData(),
                    proof.getContentType());
        } catch (final RuntimeException e) {
            LOGGER.error("Could not trigger payment proof submitted email for booking {}.", booking.getId(), e);
        }
    }

    private void sendPaymentProofRefusedEmail(
            final BookingRequest bookingRequest, final int ownerId, final String reason) {
        try {
            final Optional<Item> item = bookingRequest.getItemId() == null
                    ? Optional.empty()
                    : itemDao.findAnyItemById(bookingRequest.getItemId());
            final String ownerName =
                    itemDao.findUserById(ownerId).map(User::getName).orElse("");
            mailService.sendPaymentProofRefusedEmail(
                    bookingRequest.getRequesterEmail(),
                    bookingRequest.getRequesterLocaleTag(),
                    ownerName,
                    item.map(Item::getTitle).orElse(""),
                    reason);
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not trigger payment proof refused email for booking token {}.",
                    bookingRequest.getToken(),
                    e);
        }
    }

    private void sendPaymentReceivedEmail(final BookingRequest bookingRequest) {
        try {
            final Optional<Item> item = bookingRequest.getItemId() == null
                    ? Optional.empty()
                    : itemDao.findAnyItemById(bookingRequest.getItemId());
            mailService.sendPaymentReceivedEmail(
                    bookingRequest.getRequesterEmail(),
                    bookingRequest.getRequesterLocaleTag(),
                    item.map(Item::getTitle).orElse(""));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not trigger payment received email for booking token {}.", bookingRequest.getToken(), e);
        }
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

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED
                || booking.getState() == BookingState.BOOKING_PAYMENT_SUBMITTED
                || booking.getState() == BookingState.BOOKING_PAID;
    }

    private static boolean rangesOverlap(
            final OffsetDateTime aStart,
            final OffsetDateTime aEnd,
            final OffsetDateTime bStart,
            final OffsetDateTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            return false;
        }
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    @Override
    public boolean canAccessPaymentProof(final int bookingId, final int viewerUserId) {
        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()) {
            return false;
        }
        if (booking.get().getGuestId() != null && booking.get().getGuestId().equals(viewerUserId)) {
            return true;
        }
        if (booking.get().getItemId() == null) {
            return false;
        }
        final Optional<Item> item = itemDao.findAnyItemById(booking.get().getItemId());
        return item.isPresent()
                && item.get().getOwnerId() != null
                && item.get().getOwnerId().equals(viewerUserId);
    }

    @Override
    public BlockSlotOutcome blockSlotForOwner(
            final int itemId, final int ownerId, final String date, final String startTime, final String endTime) {
        if (date == null || startTime == null || endTime == null) {
            return BlockSlotOutcome.INVALID;
        }
        final LocalDate parsedDate;
        final LocalTime parsedStart;
        final LocalTime parsedEnd;
        try {
            parsedDate = LocalDate.parse(date);
            parsedStart = LocalTime.parse(startTime);
            parsedEnd = LocalTime.parse(endTime);
        } catch (final DateTimeParseException exception) {
            return BlockSlotOutcome.INVALID;
        }
        if (parsedDate.isBefore(LocalDate.now())) {
            return BlockSlotOutcome.PAST_DATE;
        }
        final ZoneId zone = ZoneId.systemDefault();
        final OffsetDateTime startOdt =
                LocalDateTime.of(parsedDate, parsedStart).atZone(zone).toOffsetDateTime();
        final OffsetDateTime endOdt =
                LocalDateTime.of(parsedDate, parsedEnd).atZone(zone).toOffsetDateTime();
        try {
            createOwnerSelfBlock(itemId, ownerId, startOdt, endOdt);
        } catch (final OverlappingActiveBookingException overlap) {
            return BlockSlotOutcome.OVERLAP;
        } catch (final IllegalArgumentException invalid) {
            return BlockSlotOutcome.INVALID;
        }
        return BlockSlotOutcome.BLOCKED;
    }
}
