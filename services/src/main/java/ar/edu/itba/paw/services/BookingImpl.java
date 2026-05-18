package ar.edu.itba.paw.services;

import static java.util.Map.entry;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.dto.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.exceptions.InvalidBookingStatusException;
import ar.edu.itba.paw.models.exceptions.InvalidDateFormatException;
import ar.edu.itba.paw.models.exceptions.InvalidSlotException;
import ar.edu.itba.paw.models.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.models.exceptions.PastSlotException;
import ar.edu.itba.paw.models.exceptions.SlotOverlapException;
import ar.edu.itba.paw.persistence.BookingDao;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingImpl implements BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingImpl.class);

    private final BookingDao bookingDao;
    private final ItemService itemInterface;

    private static final int MIN_ANTICIPATION_MINUTES = 120;

    private static final Map<BookingStatusEnum, EnumSet<BookingStatusEnum>> VALID_TRANSITIONS = Map.ofEntries(
            entry(
                    BookingStatusEnum.PENDING,
                    EnumSet.of(BookingStatusEnum.ACCEPTED, BookingStatusEnum.REJECTED, BookingStatusEnum.CANCELLED)),
            entry(BookingStatusEnum.ACCEPTED, EnumSet.of(BookingStatusEnum.PAID, BookingStatusEnum.CANCELLED)),
            entry(
                    BookingStatusEnum.PAID,
                    EnumSet.of(BookingStatusEnum.REFUSED, BookingStatusEnum.CONFIRMED, BookingStatusEnum.CANCELLED)),
            entry(BookingStatusEnum.REFUSED, EnumSet.of(BookingStatusEnum.PAID, BookingStatusEnum.CANCELLED)),
            entry(BookingStatusEnum.CONFIRMED, EnumSet.of(BookingStatusEnum.FINISHED, BookingStatusEnum.CANCELLED)));

    private static boolean isValidTransition(BookingStatusEnum source, BookingStatusEnum target) {
        var targets = VALID_TRANSITIONS.getOrDefault(source, EnumSet.noneOf(BookingStatusEnum.class));
        return targets.contains(target);
    }

    // TODO: move current times to utils
    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime currentMinimumStart() {
        return currentDateTime().plusMinutes(MIN_ANTICIPATION_MINUTES);
    }

    private boolean hasEnoughAnticipation(int bookingId) {
        LocalDateTime minStartTime = currentMinimumStart();
        return bookingDao.startsAfter(bookingId, minStartTime);
    }

    private void finalizeBookings() {
        bookingDao.finalizeBookingsBefore(currentDateTime());
    }

    private void expireDueBookings() {
        LocalDateTime minStartTime = currentMinimumStart();
        bookingDao.expireBookingsBefore(minStartTime);
    }

    private void verifyAnticipation(int bookingId) {
        if (!hasEnoughAnticipation(bookingId)) throw new NoAnticipationException();
    }

    @Override
    @Transactional
    public int createBooking(
            final int versionId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime,
            final String message,
            final int guestId) {
        final LocalDateTime start = LocalDateTime.of(date, startTime);
        if (start.isBefore(currentMinimumStart())) throw new NoAnticipationException();

        final String timezone =
                bookingDao.findVersionTimezone(versionId).orElseThrow(OutsideAvailabilityException::new);
        final ZoneId zoneId = ZoneId.of(timezone.trim());
        final LocalDateTime localStart = LocalDateTime.of(date, startTime);
        final LocalDateTime localEnd = LocalDateTime.of(date, endTime);

        final List<Availability> availabilities = bookingDao.listAvailabilitiesForVersion(versionId);
        final DayOfWeek dayOfWeek = localStart.getDayOfWeek();
        final boolean withinAvailability = availabilities.stream()
                .anyMatch(a -> a.getWeekday().name().equals(dayOfWeek.name())
                        && !a.getStartTime().isAfter(startTime)
                        && !a.getEndTime().isBefore(endTime));
        if (!withinAvailability) {
            throw new OutsideAvailabilityException();
        }

        final ZonedDateTime zonedStart = localStart.atZone(zoneId);
        final ZonedDateTime zonedEnd = localEnd.atZone(zoneId);
        final LocalDateTime utcStart = LocalDateTime.ofInstant(zonedStart.toInstant(), ZoneOffset.UTC);
        final LocalDateTime utcEnd = LocalDateTime.ofInstant(zonedEnd.toInstant(), ZoneOffset.UTC);

        final Integer ownerId =
                bookingDao.findOwnerIdForVersion(versionId).orElseThrow(OutsideAvailabilityException::new);
        final boolean isOwner = guestId == ownerId;
        final BookingStatusEnum status = isOwner ? BookingStatusEnum.CONFIRMED : BookingStatusEnum.PENDING;

        return bookingDao
                .insertBooking(versionId, guestId, utcStart, utcEnd, status, message)
                .orElseThrow(BookingCollisionException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForVersion(final int versionId) {
        return bookingDao.getBookingsForVersion(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSearchResult searchBookings(
            final int userId,
            final boolean asHost,
            final String searchQuery,
            final String rawDate,
            final String rawStatus,
            final Integer page,
            final Integer pageSize,
            final String sortBy) {
        final LocalDate date = parseDate(rawDate);
        final BookingStatusEnum status = parseStatus(rawStatus);
        return bookingDao.searchBookings(userId, asHost, searchQuery, date, status, page, pageSize, sortBy);
    }

    private static LocalDate parseDate(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (final Exception e) {
            throw new InvalidDateFormatException(raw);
        }
    }

    private static BookingStatusEnum parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BookingStatusEnum.valueOf(raw.trim());
        } catch (final IllegalArgumentException e) {
            throw new InvalidBookingStatusException(raw);
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0,30 * * * *")
    public void bookingResolutionRoutine() {
        finalizeBookings();
        expireDueBookings();
    }

    @Override
    @Transactional
    public void acceptBooking(int bookingId, int callerId) {
        verifyAnticipation(bookingId);
        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.ACCEPTED)) {
            throw new IllegalBookingOperationException();
        }
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatusEnum.ACCEPTED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void rejectBooking(int bookingId, int callerId) {
        verifyAnticipation(bookingId);
        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.REJECTED)) {
            throw new IllegalBookingOperationException();
        }
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatusEnum.REJECTED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void submitPayment(final int bookingId, final PaymentProof payment, final int callerId) {
        if (payment == null) return;

        verifyAnticipation(bookingId);

        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.PAID)) {
            throw new IllegalBookingOperationException();
        }

        bookingDao
                .updateStatusOutgoing(bookingId, callerId, BookingStatusEnum.PAID)
                .orElseThrow(IllegalBookingOperationException::new);

        final LocalDateTime now = currentDateTime();
        payment.setCreatedAt(now);
        if (payment.getReplyMsg() != null) {
            payment.setRepliedAt(now);
        }
        payment.setBooking(booking);
        bookingDao.uploadPayment(payment).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentProof> getPaymentProofForParticipant(final int bookingId, final int callerId) {
        return bookingDao.findPaymentProofForParticipant(bookingId, callerId);
    }

    @Override
    @Transactional
    public void confirmPayment(int bookingId, int callerId) {
        verifyAnticipation(bookingId);
        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.CONFIRMED)) {
            throw new IllegalBookingOperationException();
        }
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatusEnum.CONFIRMED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void rejectPayment(int bookingId, int callerId, String reason) {
        verifyAnticipation(bookingId);
        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.REFUSED)) {
            throw new IllegalBookingOperationException();
        }
        var now = currentDateTime();
        bookingDao
                .updateStatusIncoming(bookingId, callerId, BookingStatusEnum.REFUSED)
                .orElseThrow(IllegalBookingOperationException::new);
        bookingDao.refusePayment(bookingId, reason, now).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void cancelBooking(int bookingId, int callerId) {
        final Booking booking = bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
        if (!isValidTransition(booking.getStatus(), BookingStatusEnum.CANCELLED)) {
            throw new IllegalBookingOperationException();
        }
        bookingDao
                .updateStatusOutgoing(bookingId, callerId, BookingStatusEnum.CANCELLED)
                .orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerAvailabilityPage loadOwnerAvailabilityPage(
            final int itemId, final int ownerId, final String requestedDate) {
        final MyBoatsItem item = itemInterface.requireOwnedItem(itemId, ownerId);
        final int versionId = item.getVersionId();

        final String timezone = bookingDao.findVersionTimezone(versionId).orElse("UTC");
        final List<Availability> availabilities = bookingDao.listAvailabilitiesForVersion(versionId);
        final List<Booking> bookings = bookingDao.getBookingsForVersion(versionId);

        final List<String> offeredDates = buildOfferedDates(availabilities, timezone);
        final List<Booking> selfBlocks = bookings.stream()
                .filter(b -> b.getGuest() != null && b.getGuest().getId().equals(ownerId))
                .filter(b ->
                        b.getStatus() == BookingStatusEnum.CONFIRMED || b.getStatus() == BookingStatusEnum.ACCEPTED)
                .toList();
        final List<String> blockedDates = selfBlocks.stream()
                .map(b -> b.getStart().atZone(ZoneId.of(timezone)).toLocalDate().toString())
                .distinct()
                .sorted()
                .toList();

        final String selectedDate =
                requestedDate != null && !requestedDate.isBlank() && offeredDates.contains(requestedDate)
                        ? requestedDate
                        : (offeredDates.isEmpty() ? null : offeredDates.get(0));

        return new OwnerAvailabilityPage(
                item, availabilities, bookings, selfBlocks, offeredDates, blockedDates, selectedDate, timezone);
    }

    @Override
    @Transactional
    public void blockSlotForOwner(
            final int itemId, final int ownerId, final String date, final String startTime, final String endTime) {
        if (date == null || startTime == null || endTime == null) {
            throw new InvalidSlotException();
        }
        final LocalDate parsedDate;
        final LocalTime parsedStart;
        final LocalTime parsedEnd;
        try {
            parsedDate = LocalDate.parse(date);
            parsedStart = LocalTime.parse(startTime);
            parsedEnd = LocalTime.parse(endTime);
        } catch (final Exception e) {
            throw new InvalidSlotException();
        }
        if (parsedDate.isBefore(LocalDate.now())) {
            throw new PastSlotException();
        }
        final MyBoatsItem item = itemInterface.requireOwnedItem(itemId, ownerId);
        final int versionId = item.getVersionId();

        final String timezone = bookingDao.findVersionTimezone(versionId).orElse(null);
        if (timezone == null) {
            throw new InvalidSlotException();
        }
        final ZoneId zone = ZoneId.of(timezone.trim());
        final OffsetDateTime startOdt =
                LocalDateTime.of(parsedDate, parsedStart).atZone(zone).toOffsetDateTime();
        final OffsetDateTime endOdt =
                LocalDateTime.of(parsedDate, parsedEnd).atZone(zone).toOffsetDateTime();
        final LocalDateTime utcStart =
                startOdt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        final LocalDateTime utcEnd =
                endOdt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();

        final Optional<Integer> id =
                bookingDao.insertBooking(versionId, ownerId, utcStart, utcEnd, BookingStatusEnum.CONFIRMED, null);
        if (id.isEmpty()) {
            throw new SlotOverlapException();
        }
    }

    @Override
    @Transactional
    public boolean removeOwnerSelfBlock(final int bookingId, final int ownerId) {
        final Optional<Booking> bookingOpt = bookingDao.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return false;
        }
        final Booking booking = bookingOpt.get();
        if (booking.getGuest() == null || !booking.getGuest().getId().equals(ownerId)) {
            throw new ForbiddenOperationException();
        }
        return bookingDao.deleteOwnerSelfBlock(bookingId, ownerId);
    }

    private static List<String> buildOfferedDates(final List<Availability> availabilities, final String timezone) {
        final ZoneId zone = ZoneId.of(timezone);
        final LocalDate today = LocalDate.now(zone);
        final LocalDate end = today.plusDays(60);
        final List<String> dates = new ArrayList<>();
        LocalDate cursor = today;
        while (!cursor.isAfter(end)) {
            final DayOfWeek dow = cursor.getDayOfWeek();
            final boolean hasAvailability =
                    availabilities.stream().anyMatch(a -> a.getWeekday().name().equals(dow.name()));
            if (hasAvailability) {
                dates.add(cursor.toString());
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }
}
