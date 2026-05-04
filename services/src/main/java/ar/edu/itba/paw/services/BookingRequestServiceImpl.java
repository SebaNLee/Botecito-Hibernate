package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemBookingDao;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.utils.PaymentProofValidator;
import ar.edu.itba.paw.services.utils.UserNameRules;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final ItemBookingDao itemBookingDao;
    private final UserDao userDao;
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
        final ItemBooking booking = itemBookingDao.createBookingRequest(
                itemId, requesterUser.getId(), startTime, endTime, description, token);
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
        itemBookingDao
                .findBookingByHostDecisionToken(hostDecisionToken)
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
        for (final ItemBooking booking : itemBookingDao.listBookingsByItemId(item.getId())) {
            if (isBlockingBooking(booking)
                    && rangesOverlap(startTime, endTime, booking.getStartTime(), booking.getEndTime())) {
                throw new OverlappingActiveBookingException();
            }
        }
        final OffsetDateTime recordedAt = OffsetDateTime.now();
        return itemBookingDao.insertOwnerPersonalBlock(
                itemId, ownerId, startTime, endTime, UUID.randomUUID().toString(), recordedAt);
    }

    @Override
    @Transactional
    public boolean removeOwnerSelfBlock(final int bookingId, final int ownerId) {
        final Optional<ItemBooking> bookingOpt = itemBookingDao.findBookingById(bookingId);
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
        return itemBookingDao.deleteOwnerSelfBlock(bookingId, ownerId);
    }

    @Override
    public Optional<BookingRequest> findByToken(final String token) {
        return itemBookingDao.findBookingByHostDecisionToken(token).flatMap(this::toBookingRequest);
    }

    @Override
    public Optional<BookingRequest> resolveBookingRequest(final String token, final BookingState newStatus) {
        expireAllDue(currentDateTime());
        LOGGER.info("Resolving booking request to status {}", newStatus);
        if (newStatus == BookingState.BOOKING_CONFIRMED) {
            validateAnticipationByToken(token);
        }
        if (!itemBookingDao.resolveBookingByHostDecisionToken(token, newStatus, currentDateTime())) {
            return Optional.empty();
        }
        final Optional<BookingRequest> resolved = findByToken(token);
        resolved.ifPresent(this::sendBookingResolutionEmail);
        return resolved;
    }

    @Override
    public BookingResolutionOutcome resolveBookingRequestInAccount(
            final int bookingId, final int ownerId, final BookingState newStatus) {
        try {
            final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);
            if (booking.isEmpty()
                    || booking.get().getHostDecisionToken() == null
                    || booking.get().getItemId() == null) {
                return BookingResolutionOutcome.ERROR;
            }
            final Optional<Item> item = itemDao.findAnyItemById(booking.get().getItemId());
            if (item.isEmpty()
                    || item.get().getOwnerId() == null
                    || !item.get().getOwnerId().equals(ownerId)) {
                return BookingResolutionOutcome.ERROR;
            }
            final Optional<BookingRequest> resolved =
                    resolveBookingRequest(booking.get().getHostDecisionToken(), newStatus);
            if (resolved.isEmpty()) {
                return BookingResolutionOutcome.ERROR;
            }
            return newStatus == BookingState.BOOKING_CONFIRMED
                    ? BookingResolutionOutcome.ACCEPTED
                    : BookingResolutionOutcome.REJECTED;
        } catch (final RuntimeException exception) {
            LOGGER.error("Could not resolve booking {} in account for owner {}", bookingId, ownerId, exception);
            return BookingResolutionOutcome.ERROR;
        }
    }

    @Override
    @Transactional
    public void expireAllDue(final OffsetDateTime currentDateTime) {
        itemBookingDao.expireAllDueBookings(currentDateTime.plusMinutes(MIN_ANTICIPATION_MINUTES));
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

        final List<ItemBooking> candidateBookings = itemBookingDao.findBookingsByHostDecisionTokens(normalizedTokens);
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
        itemBookingDao.resolveBookingsByHostDecisionTokens(pendingTokens, newStatus, resolvedAt);
        LOGGER.info("Resolved {} booking requests to status {}", pendingTokens.size(), newStatus);

        final List<ItemBooking> updatedBookings = itemBookingDao.findBookingsByHostDecisionTokens(pendingTokens);
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
        for (final User user : userDao.findUsersByIds(guestIds)) {
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
        final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);
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
            if (itemBookingDao.findPaymentProofByBookingId(bookingId).isPresent()) {
                LOGGER.warn("Payment proof already exists for booking {}", bookingId);
                return Optional.empty();
            }
            if (!itemBookingDao.markBookingPaymentSubmitted(bookingId, requesterId)) {
                LOGGER.error("Failed to mark booking {} as payment submitted", bookingId);
                return Optional.empty();
            }
        } else if (state == BookingState.BOOKING_PAYMENT_REFUSED) {
            itemBookingDao.deletePaymentProofByBookingId(bookingId);
            if (!itemBookingDao.markBookingPaymentResubmitted(bookingId, requesterId)) {
                LOGGER.error("Failed to mark booking {} as payment resubmitted", bookingId);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("Invalid state {} for payment submission on booking {}", state, bookingId);
            return Optional.empty();
        }

        LOGGER.info("Submitting payment proof for booking {}", bookingId);

        final BookingPaymentProof proof = itemBookingDao.createPaymentProof(
                bookingId, requesterId, fileName, contentType, fileData, normalizeReply(guestReply));
        sendPaymentProofSubmittedEmail(booking.get(), requesterId, proof);
        return Optional.of(proof);
    }

    @Override
    public PaymentProofSubmissionOutcome submitPaymentProofInAccount(
            final int bookingId,
            final int requesterId,
            final InputStream fileContent,
            final String originalFilename,
            final String contentType,
            final String guestReply) {
        final byte[] fileData;
        try (InputStream in = fileContent == null ? InputStream.nullInputStream() : fileContent) {
            fileData = readPaymentProofUploadBytesCapped(in);
        } catch (final IOException exception) {
            LOGGER.error(
                    "Could not read payment proof upload for booking {} and requester {}",
                    bookingId,
                    requesterId,
                    exception);
            return PaymentProofSubmissionOutcome.ERROR;
        }
        final String fileName = PaymentProofValidator.sanitizeUploadedBaseName(originalFilename);
        return submitPaymentProofInAccountWithBytes(
                bookingId, requesterId, fileName, contentType, fileData, guestReply);
    }

    private PaymentProofSubmissionOutcome submitPaymentProofInAccountWithBytes(
            final int bookingId,
            final int requesterId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply) {
        try {
            if (!PaymentProofValidator.isValid(fileName, contentType, fileData)) {
                return PaymentProofSubmissionOutcome.INVALID_FILE;
            }
            final boolean hadExistingProof =
                    findPaymentProofByBookingId(bookingId).isPresent();
            final Optional<BookingPaymentProof> proof =
                    submitPaymentProof(bookingId, requesterId, fileName, contentType, fileData, guestReply);
            if (proof.isEmpty()) {
                return PaymentProofSubmissionOutcome.INVALID_FILE;
            }
            return hadExistingProof
                    ? PaymentProofSubmissionOutcome.RESUBMITTED
                    : PaymentProofSubmissionOutcome.SUBMITTED;
        } catch (final RuntimeException exception) {
            LOGGER.error(
                    "Could not submit payment proof in account for booking {} and requester {}",
                    bookingId,
                    requesterId,
                    exception);
            return PaymentProofSubmissionOutcome.ERROR;
        }
    }

    private static byte[] readPaymentProofUploadBytesCapped(final InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > PaymentProofValidator.MAX_FILE_SIZE_BYTES) {
                throw new IOException("Payment proof exceeds maximum size");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    @Override
    public Optional<BookingRequest> refusePaymentProof(final int bookingId, final int ownerId, final String reason) {
        expireAllDue(currentDateTime());
        final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);

        LOGGER.debug("Refusing payment proof for booking {} by owner {}", bookingId, ownerId);

        if (booking.isEmpty()
                || booking.get().getItemId() == null
                || booking.get().getState() != BookingState.BOOKING_PAYMENT_SUBMITTED
                || itemBookingDao.findPaymentProofByBookingId(bookingId).isEmpty()) {
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

        if (!itemBookingDao.markBookingPaymentRefused(bookingId, ownerId, trimmed)) {
            return Optional.empty();
        }
        final Optional<BookingRequest> refused =
                itemBookingDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
        refused.ifPresent(request -> sendPaymentProofRefusedEmail(request, ownerId, trimmed));
        return refused;
    }

    @Override
    public PaymentRefusalOutcome refusePaymentProofInAccount(
            final int bookingId, final int ownerId, final String reason) {
        try {
            final Optional<BookingRequest> refused = refusePaymentProof(bookingId, ownerId, reason);
            return refused.isPresent() ? PaymentRefusalOutcome.REFUSED : PaymentRefusalOutcome.ERROR;
        } catch (final RuntimeException exception) {
            LOGGER.error(
                    "Could not refuse payment proof in account for booking {} and owner {}",
                    bookingId,
                    ownerId,
                    exception);
            return PaymentRefusalOutcome.ERROR;
        }
    }

    @Override
    public Optional<BookingRequest> confirmPaymentReceived(final int bookingId, final int ownerId) {
        expireAllDue(currentDateTime());
        final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);

        LOGGER.debug("Confirming payment for booking {} by owner {}", bookingId, ownerId);

        if (booking.isEmpty()
                || booking.get().getItemId() == null
                || booking.get().getHostDecisionToken() == null
                || booking.get().getState() != BookingState.BOOKING_PAYMENT_SUBMITTED
                || itemBookingDao.findPaymentProofByBookingId(bookingId).isEmpty()) {
            return Optional.empty();
        }

        final Optional<Item> item = itemDao.findAnyItemById(booking.get().getItemId());
        if (item.isEmpty()
                || item.get().getOwnerId() == null
                || !item.get().getOwnerId().equals(ownerId)) {
            return Optional.empty();
        }

        if (!itemBookingDao.markBookingPaid(bookingId, ownerId)) {
            return Optional.empty();
        }
        final Optional<BookingRequest> paid =
                itemBookingDao.findBookingById(bookingId).flatMap(this::toBookingRequest);
        paid.ifPresent(this::sendPaymentReceivedEmail);
        return paid;
    }

    @Override
    public PaymentConfirmationOutcome confirmPaymentReceivedInAccount(final int bookingId, final int ownerId) {
        try {
            final Optional<BookingRequest> paid = confirmPaymentReceived(bookingId, ownerId);
            return paid.isPresent() ? PaymentConfirmationOutcome.CONFIRMED : PaymentConfirmationOutcome.ERROR;
        } catch (final RuntimeException exception) {
            LOGGER.error(
                    "Could not confirm payment in account for booking {} and owner {}", bookingId, ownerId, exception);
            return PaymentConfirmationOutcome.ERROR;
        }
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId) {
        return itemBookingDao.findPaymentProofByBookingId(bookingId);
    }

    private User resolveOrCreateRequesterUser(
            final String requesterGivenName,
            final String requesterLastName,
            final String requesterEmail,
            final String requesterPreferredLanguage) {
        UserNameRules.requireBothLegalNames(requesterGivenName, requesterLastName);
        final String givenName = normalizeNamePart(requesterGivenName, "Guest");
        final String lastName = normalizeNamePart(requesterLastName, "");
        final String preferredLanguage = normalizePreferredLanguage(requesterPreferredLanguage);

        final Optional<User> existingUser = userDao.findByEmail(requesterEmail);
        if (existingUser.isPresent()) {
            final User user = existingUser.get();
            userDao.updateBasicProfileNamesAndLanguage(user.getId(), givenName, lastName, preferredLanguage);
            user.setGivenName(givenName);
            user.setLastName(lastName);
            user.setPreferredLanguage(ar.edu.itba.paw.models.PreferredLanguage.fromPersistence(preferredLanguage));
            return user;
        }

        return userDao.createUserWithoutCredentials(givenName, lastName, requesterEmail, preferredLanguage);
    }

    private Optional<BookingRequest> toBookingRequest(final ItemBooking booking) {
        return userDao.findById(booking.getGuestId()).map(user -> toBookingRequest(booking, user));
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
                    : userDao.findById(item.getOwnerId()).map(User::getEmail).orElse(null);
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
            final Optional<User> owner = userDao.findById(item.get().getOwnerId());
            if (owner.isEmpty()) {
                return;
            }
            final String requesterName =
                    userDao.findById(requesterId).map(User::getName).orElse("");
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
                    userDao.findById(ownerId).map(User::getName).orElse("");
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
        final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);
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
