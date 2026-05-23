package ar.edu.itba.paw.services;

import static java.util.Map.entry;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.dto.OwnerAvailabilityPage;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.exceptions.InvalidBookingStatusException;
import ar.edu.itba.paw.models.exceptions.InvalidDateFormatException;
import ar.edu.itba.paw.models.exceptions.InvalidSlotException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.models.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.models.exceptions.PastSlotException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.DetailDao;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final DetailDao detailDao; // TODO: temporal hasta nuevo item service
    private final UserService userService;

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

    private static final EnumSet<BookingStatusEnum> AUTO_CANCEL_STATES = EnumSet.of(
            BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED, BookingStatusEnum.PAID, BookingStatusEnum.REFUSED);

    private static final EnumSet<BookingStatusEnum> BLOCKING_STATES = EnumSet.of(
            BookingStatusEnum.PENDING,
            BookingStatusEnum.ACCEPTED,
            BookingStatusEnum.PAID,
            BookingStatusEnum.REFUSED,
            BookingStatusEnum.CONFIRMED);

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

    private boolean startsAfter(Booking booking, LocalDateTime time) {
        return booking != null && booking.getStart().isAfter(time);
    }

    private boolean hasEnoughAnticipation(Booking booking) {
        LocalDateTime minStartTime = currentMinimumStart();
        return startsAfter(booking, minStartTime);
    }

    private void finalizeBookings() {
        bookingDao.finalizeBookingsBefore(currentDateTime());
    }

    private void expireDueBookings() {
        LocalDateTime minStartTime = currentMinimumStart();
        bookingDao.expireBookingsBefore(minStartTime, AUTO_CANCEL_STATES);
    }

    private void verifyAnticipation(Booking booking) {
        if (!hasEnoughAnticipation(booking)) throw new NoAnticipationException();
    }

    @Override
    @Transactional
    public void createBooking(
            final int itemId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime,
            final String message,
            final int callerId) {
        insertBooking(itemId, date, startTime, endTime, message, callerId, true);
    }

    private void insertBooking(
            final int itemId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime,
            final String message,
            final int guestId,
            final boolean checkAnticipation) {
        final LocalDateTime start = LocalDateTime.of(date, startTime);
        final LocalDateTime now = currentDateTime();
        if (start.isBefore(now)) throw new PastSlotException();
        if (checkAnticipation && start.isBefore(currentMinimumStart())) throw new NoAnticipationException();

        Users guest = userService.findById(guestId).orElseThrow(ForbiddenOperationException::new);

        Item item = detailDao.getItemDetail(itemId, 1).orElseThrow(ItemNotFoundException::new);
        if (item.getStatus() != ItemStatusEnum.ACTIVE) throw new ItemNotFoundException();

        Version version = item.getLatestVersion();

        final String timezone = version.getTimezone();
        final ZoneId zoneId = ZoneId.of(timezone.trim());
        final LocalDateTime localStart = LocalDateTime.of(date, startTime);
        final LocalDateTime localEnd = LocalDateTime.of(date, endTime);

        final List<Availability> availabilities = version.getAvailabilities();
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

        final Integer ownerId = item.getHost().getId();
        final boolean isOwner = guestId == ownerId;
        final BookingStatusEnum status = isOwner ? BookingStatusEnum.CONFIRMED : BookingStatusEnum.PENDING;

        Booking booking = Booking.builder()
                .version(version)
                .guest(guest)
                .start(utcStart)
                .end(utcEnd)
                .status(status)
                .msg(message)
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (bookingDao.canInsertBooking(booking, BLOCKING_STATES)) {
            bookingDao.insertBooking(booking);
        } else throw new BookingCollisionException();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSearchResult searchBookings(
            final int callerId,
            final boolean asHost,
            final String searchQuery,
            final String rawDate,
            final String rawStatus,
            final Integer page,
            final Integer pageSize,
            final String sortBy) {
        final LocalDate date = parseDate(rawDate);
        final BookingStatusEnum status = parseStatus(rawStatus);

        var query = BookingQueryModel.builder()
                .callerId(callerId)
                .asHost(asHost)
                .searchQuery(searchQuery)
                .date(date)
                .status(status)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .build();

        return bookingDao.searchBookings(query);
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

    @Transactional
    @Scheduled(cron = "0 0,30 * * * *")
    public void bookingResolutionRoutine() {
        finalizeBookings();
        expireDueBookings();
    }

    // Lanza excepcion si falta anticipacion, si al caller no le corresponde actuar, o si la transicion de estado no es
    // posible
    // asHost == false se interpreta como "as guest"
    private void updateStatus(
            Booking booking, int callerId, boolean asHost, BookingStatusEnum newStatus, boolean checkAnticipation) {
        if (checkAnticipation) verifyAnticipation(booking);

        boolean legal = asHost
                ? booking.getVersion().getItem().getHost().getId() == callerId
                : booking.getGuest().getId() == callerId;

        if (!legal || !isValidTransition(booking.getStatus(), newStatus)) {
            throw new IllegalBookingOperationException();
        }

        booking.setStatus(newStatus);
        booking.setUpdatedAt(currentDateTime());
    }

    private void updateStatus(Booking booking, int callerId, boolean asHost, BookingStatusEnum newStatus) {
        updateStatus(booking, callerId, asHost, newStatus, true);
    }

    private void updateStatus(int bookingId, int callerId, boolean asHost, BookingStatusEnum newStatus) {
        updateStatus(findById(bookingId), callerId, asHost, newStatus);
    }

    private Booking findById(int bookingId) {
        return bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void acceptBooking(int bookingId, int callerId) {
        updateStatus(bookingId, callerId, true, BookingStatusEnum.ACCEPTED);
    }

    @Override
    @Transactional
    public void rejectBooking(int bookingId, int callerId) {
        updateStatus(bookingId, callerId, true, BookingStatusEnum.REJECTED);
    }

    @Override
    @Transactional
    public void submitPayment(
            final int bookingId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final int callerId) {
        Booking booking = findById(bookingId);
        updateStatus(booking, callerId, false, BookingStatusEnum.PAID);

        var payment = PaymentProof.builder()
                .createdAt(currentDateTime())
                .booking(booking)
                .filename(fileName)
                .contentType(contentType)
                .fileData(fileData)
                .build();

        bookingDao.uploadPayment(payment);
    }

    @Override
    @Transactional
    public void updatePayment(
            final int bookingId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String replyMsg,
            final int callerId) {
        PaymentProof payment = findById(bookingId).getPaymentProof();
        if (payment == null) {
            // Failsafe
            submitPayment(bookingId, fileName, contentType, fileData, callerId);
            return;
        }

        payment.setFilename(fileName);
        payment.setContentType(contentType);
        payment.setFileData(fileData);
        payment.setReplyMsg(replyMsg);
        payment.setRepliedAt(currentDateTime());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentProof> getPaymentProofForParticipant(final int bookingId, final int callerId) {
        return Optional.ofNullable(findById(bookingId).getPaymentProof());
    }

    @Override
    @Transactional
    public void confirmPayment(int bookingId, int callerId) {
        updateStatus(bookingId, callerId, true, BookingStatusEnum.CONFIRMED);
    }

    @Override
    @Transactional
    public void rejectPayment(int bookingId, int callerId, String reason) {
        Booking booking = findById(bookingId);
        PaymentProof payment = booking.getPaymentProof();
        updateStatus(booking, callerId, true, BookingStatusEnum.REFUSED);

        payment.setRefuseMsg(reason);
        payment.setRefusedAt(currentDateTime());
    }

    @Override
    @Transactional
    public void cancelBooking(int bookingId, int callerId) {
        updateStatus(findById(bookingId), callerId, false, BookingStatusEnum.CANCELLED, false);
    }

    // TODO: full remake, remove temp fix
    @Override
    @Transactional(readOnly = true)
    public OwnerAvailabilityPage loadOwnerAvailabilityPage(
            final int itemId, final int callerId, final String requestedDate) {

        Item item = detailDao.getItemDetail(itemId, 1).orElseThrow(ItemNotFoundException::new);
        if (item.getHost().getId() != callerId) throw new ForbiddenOperationException();
        Version version = item.getLatestVersion();

        final String timezone = version.getTimezone();
        final List<Availability> availabilities = version.getAvailabilities();
        final List<Booking> bookings = item.getBookings();

        final List<String> offeredDates = buildOfferedDates(availabilities, timezone);
        final List<Booking> selfBlocks = bookings.stream()
                .filter(b -> b.getGuest() != null && b.getGuest().getId().equals(callerId))
                .filter(b ->
                        b.getStatus() == BookingStatusEnum.CONFIRMED || b.getStatus() == BookingStatusEnum.ACCEPTED)
                .toList();
        final ZoneId zone = ZoneId.of(timezone);
        final List<String> blockedDates = selfBlocks.stream()
                .map(b -> b.getStart()
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(zone)
                        .toLocalDate()
                        .toString())
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
            final int itemId, final int callerId, final String date, final String startTime, final String endTime) {
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

        Item item = detailDao.getItemDetail(itemId, 1).orElseThrow(ItemNotFoundException::new);
        if (item.getHost().getId() != callerId) throw new ForbiddenOperationException();

        insertBooking(itemId, parsedDate, parsedStart, parsedEnd, "", callerId, false);
    }

    @Override
    @Transactional
    public boolean removeOwnerSelfBlock(final int bookingId, final int callerId) {
        Booking booking = findById(bookingId);
        var ownerId = booking.getVersion().getItem().getHost().getId().intValue();
        if (ownerId != callerId || ownerId != booking.getGuest().getId().intValue()) return false;
        bookingDao.deleteBooking(bookingId);
        return true;
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
