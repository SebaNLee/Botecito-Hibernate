package ar.edu.itba.paw.services;

import static java.util.Map.entry;

import ar.edu.itba.paw.models.booking.BookingBlockingStatuses;
import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.SelfBlockCreate;
import ar.edu.itba.paw.models.dto.SelfBlockUpdate;
import ar.edu.itba.paw.models.dto.SelfBookingData;
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
import ar.edu.itba.paw.models.exceptions.InvalidPaymentProofException;
import ar.edu.itba.paw.models.exceptions.InvalidSlotException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.models.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.models.exceptions.PastSlotException;
import ar.edu.itba.paw.models.exceptions.SelfBlockCollisionException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.services.util.DateTimeUtils;
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
    private final ItemService itemService;
    private final UserService userService;
    private final MailService mailService;
    private final AvailabilityService availabilityService;

    private static final int MIN_ANTICIPATION_MINUTES = 120;
    private static final int BOOKING_LOOKAHEAD_DAYS = 60;

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

    private static boolean isValidTransition(BookingStatusEnum source, BookingStatusEnum target) {
        var targets = VALID_TRANSITIONS.getOrDefault(source, EnumSet.noneOf(BookingStatusEnum.class));
        return targets.contains(target);
    }

    private LocalDateTime currentDateTime() {
        return DateTimeUtils.getCurrent();
    }

    private LocalDateTime currentMinimumStart() {
        return currentDateTime().plusMinutes(MIN_ANTICIPATION_MINUTES);
    }

    private boolean startsAfter(Booking booking, LocalDateTime time) {
        return booking != null && booking.getStart().isAfter(time);
    }

    private boolean hasEnoughAnticipation(Booking booking) {
        return startsAfter(booking, currentMinimumStart());
    }

    private void finalizeBookings() {
        bookingDao.finalizeBookingsBefore(currentDateTime());
    }

    private void expireDueBookings() {
        bookingDao.expireBookingsBefore(currentMinimumStart(), AUTO_CANCEL_STATES);
    }

    private void deleteOldSelfBlocks() {
        bookingDao.deleteSelfBlocksBefore(currentDateTime());
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
        Users guest = userService.findById(guestId).orElseThrow(ForbiddenOperationException::new);

        Item item = itemService.findItemById(itemId);
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

        final LocalDateTime now = currentDateTime();
        if (utcStart.isBefore(now)) throw new PastSlotException();
        if (checkAnticipation && utcStart.isBefore(currentMinimumStart())) throw new NoAnticipationException();

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

        boolean canInsert = bookingDao.canInsertBooking(booking, BookingBlockingStatuses.collisionBlockingStates());

        if (canInsert) {
            bookingDao.insertBooking(booking);
            LOGGER.info("Booking created: item {}, guest {}, {} - {}", itemId, guestId, utcStart, utcEnd);
            if (!isOwner) {
                mailService.sendPreBookingMail(booking);
            }
        } else if (isOwner) {
            throw new SelfBlockCollisionException();
        } else {
            throw new BookingCollisionException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageModel<Booking> searchBookings(
            final int callerId,
            final boolean asHost,
            final String searchQuery,
            final LocalDate date,
            final BookingStatusEnum status,
            final Integer page,
            final Integer pageSize,
            final String sortBy) {
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

    // Retorna una lista finita con cota superior (desde hoy hasta
    // BOOKING_LOOKAHEAD_DAYS) de bookings para un item. Los profes en la práctica
    // nos permitieron no paginarla por este motivo.
    @Transactional(readOnly = true)
    public List<Booking> getUpcomingBookings(Item item) {
        LocalDateTime now = currentDateTime();
        return bookingDao.getUpcomingBookings(item, now, now.plusDays(BOOKING_LOOKAHEAD_DAYS));
    }

    @Transactional
    @Scheduled(cron = "0 0,30 * * * *")
    public void bookingResolutionRoutine() {
        final LocalDateTime now = currentDateTime();
        final LocalDateTime expireThreshold = currentMinimumStart();
        bookingDao.findBookingsToFinalizeBefore(now).forEach(mailService::sendBookingFinishedMail);
        bookingDao.findBookingsToExpireBefore(expireThreshold).forEach(mailService::sendBookingExpiredMail);
        finalizeBookings();
        expireDueBookings();
        deleteOldSelfBlocks();
    }

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

    @Override
    @Transactional(readOnly = true)
    public Booking findById(int bookingId) {
        return bookingDao.findById(bookingId).orElseThrow(IllegalBookingOperationException::new);
    }

    @Override
    @Transactional
    public void acceptBooking(int bookingId, int callerId) {
        Booking booking = findById(bookingId);
        updateStatus(booking, callerId, true, BookingStatusEnum.ACCEPTED);
        LOGGER.info("Booking {} accepted by user {}", bookingId, callerId);
        mailService.sendAcceptMail(booking);
    }

    @Override
    @Transactional
    public void rejectBooking(int bookingId, int callerId) {
        Booking booking = findById(bookingId);
        updateStatus(booking, callerId, true, BookingStatusEnum.REJECTED);
        LOGGER.info("Booking {} rejected by user {}", bookingId, callerId);
        mailService.sendRejectMail(booking);
    }

    @Transactional
    public void insertPayment(
            final int bookingId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestMsg,
            final int callerId) {
        requirePaymentProofFile(fileData);
        Booking booking = findById(bookingId);
        updateStatus(booking, callerId, false, BookingStatusEnum.PAID);
        final LocalDateTime now = currentDateTime();

        var builder = PaymentProof.builder()
                .createdAt(now)
                .booking(booking)
                .filename(fileName)
                .contentType(contentType)
                .fileData(fileData);

        if (guestMsg != null) {
            builder.guestMsg(guestMsg).guestAt(now);
        }

        var payment = builder.build();
        bookingDao.uploadPayment(payment);
        booking.setPaymentProof(payment);
        LOGGER.info("Payment uploaded for booking {}", bookingId);
        mailService.sendPaymentMail(booking);
    }

    @Transactional
    public void submitPayment(
            final int bookingId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestMsg,
            final int callerId) {
        requirePaymentProofFile(fileData);
        PaymentProof payment = findById(bookingId).getPaymentProof();

        if (payment == null) {
            insertPayment(bookingId, fileName, contentType, fileData, guestMsg, callerId);
            return;
        }

        Booking booking = payment.getBooking();
        updateStatus(booking, callerId, false, BookingStatusEnum.PAID);

        payment.setFilename(fileName);
        payment.setContentType(contentType);
        payment.setFileData(fileData);
        payment.setGuestMsg(guestMsg);
        payment.setGuestAt(currentDateTime());
        payment.setHostMsg(null);
        payment.setHostAt(null);
        LOGGER.info("Payment resubmitted for booking {}", bookingId);
        mailService.sendPaymentMail(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentProof> getPaymentProofForParticipant(final int bookingId, final int callerId) {
        final Booking booking = findById(bookingId);
        final int hostId = booking.getVersion().getItem().getHost().getId().intValue();
        final int guestId = booking.getGuest().getId().intValue();
        if (callerId != hostId && callerId != guestId) {
            return Optional.empty();
        }
        return Optional.ofNullable(booking.getPaymentProof());
    }

    @Override
    @Transactional
    public void confirmPayment(int bookingId, int callerId) {
        Booking booking = findById(bookingId);
        final PaymentProof payment = booking.getPaymentProof();
        if (payment != null) {
            payment.setHostMsg(null);
            payment.setHostAt(null);
        }
        updateStatus(booking, callerId, true, BookingStatusEnum.CONFIRMED);
        LOGGER.info("Payment confirmed for booking {} by user {}", bookingId, callerId);
        mailService.sendBookingConfirmedMail(booking);
    }

    @Override
    @Transactional
    public void rejectPayment(int bookingId, int callerId, String hostMsg) {
        Booking booking = findById(bookingId);
        PaymentProof payment = booking.getPaymentProof();
        updateStatus(booking, callerId, true, BookingStatusEnum.REFUSED);

        payment.setHostMsg(hostMsg);
        payment.setHostAt(currentDateTime());
        LOGGER.info("Payment rejected for booking {} by user {}", bookingId, callerId);
        mailService.sendRefusedPaymentMail(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(int bookingId, int callerId) {
        Booking booking = findById(bookingId);
        updateStatus(booking, callerId, false, BookingStatusEnum.CANCELLED, false);
        LOGGER.info("Booking {} cancelled by user {}", bookingId, callerId);
        mailService.sendBookingCancelledMail(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public SelfBookingData getSelfBlocks(final int itemId, final int callerId, final LocalDate requestedDate) {

        Item item = itemService.requireOwnedActiveItem(itemId, callerId);
        Version version = item.getLatestVersion();

        final String timezone = version.getTimezone();
        final List<Availability> availabilities = version.getAvailabilities();
        final List<Booking> bookings = getUpcomingBookings(item);

        final List<LocalDate> offeredDates = buildOfferedDates(availabilities, timezone);
        final List<Booking> selfBlocks = bookings.stream()
                .filter(b -> b.getGuest() != null && b.getGuest().getId().equals(callerId))
                .filter(b ->
                        b.getStatus() == BookingStatusEnum.CONFIRMED || b.getStatus() == BookingStatusEnum.ACCEPTED)
                .toList();
        final List<LocalDate> blockedDates = selfBlocks.stream()
                .map(b -> DateTimeUtils.inTimezone(b.getStart(), timezone).toLocalDate())
                .distinct()
                .sorted()
                .toList();

        final LocalDate selectedDate = requestedDate != null && offeredDates.contains(requestedDate)
                ? requestedDate
                : (offeredDates.isEmpty() ? null : offeredDates.get(0));

        final var dayTimeline =
                availabilityService.buildDayTimeline(selectedDate, availabilities, bookings, selfBlocks, timezone);
        final boolean hasTimelineAvailability =
                availabilityService.hasAvailabilityWindowsForDate(selectedDate, availabilities);

        return new SelfBookingData(
                item,
                bookings,
                selfBlocks,
                offeredDates,
                blockedDates,
                selectedDate,
                timezone,
                dayTimeline,
                availabilityService.listingCalendarToday(timezone),
                availabilityService.listingCalendarMaxInclusive(timezone),
                hasTimelineAvailability);
    }

    @Override
    @Transactional
    public void saveSelfBlockChanges(
            final int itemId,
            final int callerId,
            final LocalDate date,
            final List<Integer> deletedBlockIds,
            final List<SelfBlockUpdate> updates,
            final List<SelfBlockCreate> creates) {
        itemService.requireOwnedActiveItem(itemId, callerId);
        if (deletedBlockIds != null) {
            for (final Integer blockId : deletedBlockIds) {
                if (blockId != null) {
                    removeSelfBlock(blockId, callerId);
                }
            }
        }
        if (updates != null) {
            for (final SelfBlockUpdate update : updates) {
                if (update == null) {
                    continue;
                }
                updateSelfBlock(update.getBookingId(), callerId, date, update.getStartTime(), update.getEndTime());
            }
        }
        if (creates != null) {
            for (final SelfBlockCreate create : creates) {
                if (create == null) {
                    continue;
                }
                insertSelfBlock(itemId, callerId, date, create.getStartTime(), create.getEndTime());
            }
        }
    }

    private void insertSelfBlock(
            final int itemId,
            final int callerId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime) {
        if (date == null || startTime == null || endTime == null) {
            throw new InvalidSlotException();
        }

        insertBooking(itemId, date, startTime, endTime, "", callerId, false);
    }

    private boolean removeSelfBlock(final int bookingId, final int callerId) {
        Booking booking = findById(bookingId);
        var ownerId = booking.getVersion().getItem().getHost().getId().intValue();
        if (ownerId != callerId || ownerId != booking.getGuest().getId().intValue()) return false;
        bookingDao.deleteBooking(bookingId);
        LOGGER.info("Self-block {} removed by user {}", bookingId, callerId);
        return true;
    }

    private void updateSelfBlock(
            final int bookingId,
            final int callerId,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime) {
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidSlotException();
        }

        final Booking booking = findById(bookingId);
        final int ownerId = booking.getVersion().getItem().getHost().getId().intValue();
        if (ownerId != callerId || ownerId != booking.getGuest().getId().intValue()) {
            throw new IllegalBookingOperationException();
        }

        final Version version = booking.getVersion();
        final String timezone = version.getTimezone();
        final ZoneId zoneId = ZoneId.of(timezone.trim());
        final LocalDateTime localStart = LocalDateTime.of(date, startTime);

        final DayOfWeek dayOfWeek = localStart.getDayOfWeek();
        final List<Availability> availabilities = version.getAvailabilities();
        final boolean withinAvailability = availabilities.stream()
                .anyMatch(a -> a.getWeekday().name().equals(dayOfWeek.name())
                        && !a.getStartTime().isAfter(startTime)
                        && !a.getEndTime().isBefore(endTime));
        if (!withinAvailability) {
            throw new OutsideAvailabilityException();
        }

        final ZonedDateTime zonedStart = localStart.atZone(zoneId);
        final ZonedDateTime zonedEnd = LocalDateTime.of(date, endTime).atZone(zoneId);
        final LocalDateTime utcStart = LocalDateTime.ofInstant(zonedStart.toInstant(), ZoneOffset.UTC);
        final LocalDateTime utcEnd = LocalDateTime.ofInstant(zonedEnd.toInstant(), ZoneOffset.UTC);
        if (utcStart.isBefore(currentDateTime())) {
            throw new PastSlotException();
        }

        final Booking probe =
                Booking.builder().version(version).start(utcStart).end(utcEnd).build();
        if (!bookingDao.canUpdate(probe, bookingId, BookingBlockingStatuses.collisionBlockingStates())) {
            throw new SelfBlockCollisionException();
        }

        booking.setStart(utcStart);
        booking.setEnd(utcEnd);
        booking.setUpdatedAt(currentDateTime());
    }

    // Retorna una lista finita con cota superior (desde hoy hasta
    // BOOKING_LOOKAHEAD_DAYS) de fechas con disponibilidad para un item. Los profes
    // en la práctica nos permitieron no paginarla por este motivo.
    private static List<LocalDate> buildOfferedDates(final List<Availability> availabilities, final String timezone) {
        final List<Availability> windows = availabilities == null ? List.of() : availabilities;
        final ZoneId zone = ZoneId.of(timezone);
        final LocalDate today = LocalDate.now(zone);
        final LocalDate end = today.plusDays(BOOKING_LOOKAHEAD_DAYS);
        final List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = today;
        while (!cursor.isAfter(end)) {
            final DayOfWeek dow = cursor.getDayOfWeek();
            final boolean hasAvailability =
                    windows.stream().anyMatch(a -> a.getWeekday().name().equals(dow.name()));
            if (hasAvailability) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private static void requirePaymentProofFile(final byte[] fileData) {
        if (fileData == null || fileData.length == 0) {
            throw new InvalidPaymentProofException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean itemHasBookings(final Item item) {
        return bookingDao.itemHasBookings(item);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean itemHasBookings(final int itemId) {
        return itemHasBookings(itemService.findItemById(itemId));
    }

    @Override
    @Transactional
    public void deleteAllSelfBlocks(final Item item) {
        bookingDao.deleteAllSelfBlocks(item);
    }
}
