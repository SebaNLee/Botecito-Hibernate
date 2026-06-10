package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.booking.BookingBlockingStatuses;
import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.DayTimelineData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public final class AvailabilityImpl implements AvailabilityService {

    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESERVATION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public AvailabilityData buildAvailabilityData(
            final List<Availability> availabilityWindows, final List<Booking> bookings, final String versionTimezone) {
        if (availabilityWindows == null || availabilityWindows.isEmpty()) {
            return emptyData();
        }
        final ZoneId zoneId = listingZoneOrUtc(versionTimezone);
        final LocalDate rangeStart = listingCalendarToday(versionTimezone);
        final LocalDate rangeEnd = listingCalendarMaxInclusive(versionTimezone);
        final Map<String, TreeSet<String>> scheduledTimesByDate = new TreeMap<>();
        for (final Availability window : availabilityWindows) {
            if (!isUsableWindow(window)) {
                continue;
            }
            final DayOfWeek dayOfWeek = DayOfWeek.valueOf(window.getWeekday().name());
            mergeOfferedTimesForWindow(
                    scheduledTimesByDate, dayOfWeek, window.getStartTime(), window.getEndTime(), rangeStart, rangeEnd);
        }
        if (scheduledTimesByDate.isEmpty()) {
            return emptyData();
        }
        final Map<String, TreeSet<String>> bookedTimesByDate =
                buildBookedTimesByDate(bookings == null ? List.of() : bookings, zoneId, rangeStart, rangeEnd);
        final Map<String, TreeSet<String>> availableTimesByDate =
                subtractTimes(scheduledTimesByDate, bookedTimesByDate);
        final Map<String, TreeSet<String>> occupiedTimesByDate =
                subtractTimes(scheduledTimesByDate, availableTimesByDate);

        final List<String> offeredDates = new ArrayList<>();
        final List<String> occupiedDates = new ArrayList<>();
        for (final Map.Entry<String, TreeSet<String>> scheduledTimesEntry : scheduledTimesByDate.entrySet()) {
            final String date = scheduledTimesEntry.getKey();
            if (!availableTimesByDate.getOrDefault(date, new TreeSet<>()).isEmpty()) {
                offeredDates.add(date);
            } else if (!scheduledTimesEntry.getValue().isEmpty()) {
                occupiedDates.add(date);
            }
        }
        return new AvailabilityData(
                List.copyOf(offeredDates),
                List.copyOf(occupiedDates),
                toImmutableTimesByDate(availableTimesByDate),
                toImmutableTimesByDate(occupiedTimesByDate));
    }

    @Override
    public DayTimelineData buildDayTimeline(
            final LocalDate selectedDate,
            final List<Availability> availabilities,
            final List<Booking> bookings,
            final List<Booking> ownerSelfBlocks,
            final String timezone) {
        if (selectedDate == null) {
            return new DayTimelineData(List.of(), List.of(), List.of());
        }
        final ZoneId zone = listingZoneOrUtc(timezone);
        final Set<Integer> selfBlockBookingIds = ownerSelfBlocks == null
                ? Set.of()
                : ownerSelfBlocks.stream()
                        .map(Booking::getId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());

        final List<DayTimelineData.TimeRangeRow> availableRanges = new ArrayList<>();
        if (availabilities != null) {
            for (final Availability availability : availabilities) {
                if (availability.getWeekday() == null
                        || !availability
                                .getWeekday()
                                .name()
                                .equals(selectedDate.getDayOfWeek().name())
                        || availability.getStartTime() == null
                        || availability.getEndTime() == null
                        || !availability.getEndTime().isAfter(availability.getStartTime())) {
                    continue;
                }
                availableRanges.add(new DayTimelineData.TimeRangeRow(
                        availability.getStartTime().format(TIME_FMT),
                        availability.getEndTime().format(TIME_FMT)));
            }
        }

        final List<DayTimelineData.TimeRangeRow> bookedRanges = new ArrayList<>();
        if (bookings != null) {
            for (final Booking booking : bookings) {
                if (!BookingBlockingStatuses.isDisplayBlocking(booking.getStatus())
                        || booking.getStart() == null
                        || booking.getEnd() == null
                        || booking.getId() != null && selfBlockBookingIds.contains(booking.getId())) {
                    continue;
                }
                final LocalDate bookingDate = booking.getStart()
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(zone)
                        .toLocalDate();
                if (!selectedDate.equals(bookingDate)) {
                    continue;
                }
                bookedRanges.add(new DayTimelineData.TimeRangeRow(
                        booking.getStart()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()
                                .format(TIME_FMT),
                        booking.getEnd()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()
                                .format(TIME_FMT)));
            }
        }

        final List<DayTimelineData.SelfBlockRow> selfBlocks = new ArrayList<>();
        if (ownerSelfBlocks != null) {
            for (final Booking block : ownerSelfBlocks) {
                if (block.getId() == null || block.getStart() == null || block.getEnd() == null) {
                    continue;
                }
                final LocalDate blockDate = block.getStart()
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(zone)
                        .toLocalDate();
                if (!selectedDate.equals(blockDate)) {
                    continue;
                }
                selfBlocks.add(new DayTimelineData.SelfBlockRow(
                        block.getId(),
                        block.getStart()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()
                                .format(TIME_FMT),
                        block.getEnd()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()
                                .format(TIME_FMT)));
            }
        }
        return new DayTimelineData(availableRanges, bookedRanges, selfBlocks);
    }

    @Override
    public boolean hasAvailabilityWindowsForDate(
            final LocalDate selectedDate, final List<Availability> availabilities) {
        if (selectedDate == null || availabilities == null) {
            return false;
        }
        return availabilities.stream()
                .anyMatch(a -> a.getWeekday() != null
                        && a.getWeekday()
                                .name()
                                .equals(selectedDate.getDayOfWeek().name())
                        && a.getStartTime() != null
                        && a.getEndTime() != null
                        && a.getEndTime().isAfter(a.getStartTime()));
    }

    @Override
    public ZoneId listingZoneOrUtc(final String versionTimezone) {
        if (versionTimezone == null || versionTimezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(versionTimezone.trim());
        } catch (final RuntimeException ignored) {
            return ZoneOffset.UTC;
        }
    }

    @Override
    public LocalDate listingCalendarToday(final String versionTimezone) {
        return ZonedDateTime.now(listingZoneOrUtc(versionTimezone)).toLocalDate();
    }

    @Override
    public LocalDate listingCalendarMaxInclusive(final String versionTimezone) {
        return pickerEndDate(listingCalendarToday(versionTimezone));
    }

    private static AvailabilityData emptyData() {
        return new AvailabilityData(List.of(), List.of(), Map.of(), Map.of());
    }

    private static boolean isUsableWindow(final Availability window) {
        if (window.getWeekday() == null || window.getStartTime() == null || window.getEndTime() == null) {
            return false;
        }
        return window.getEndTime().isAfter(window.getStartTime());
    }

    private static void mergeOfferedTimesForWindow(
            final Map<String, TreeSet<String>> collectedTimesByDate,
            final DayOfWeek weekday,
            final LocalTime startTime,
            final LocalTime endTime,
            final LocalDate rangeStart,
            final LocalDate rangeEnd) {
        for (LocalDate currentDate = rangeStart;
                !currentDate.isAfter(rangeEnd);
                currentDate = currentDate.plusDays(1)) {
            if (currentDate.getDayOfWeek() != weekday) {
                continue;
            }
            addTimeRange(collectedTimesByDate, currentDate.format(INPUT_DATE_FORMAT), startTime, endTime);
        }
    }

    private static LocalDate pickerEndDate(final LocalDate rangeStart) {
        return rangeStart
                .plusMonths(BookingBlockingStatuses.LISTING_PICKER_MONTHS_AROUND_TODAY)
                .with(TemporalAdjusters.lastDayOfMonth());
    }

    private static Map<String, TreeSet<String>> buildBookedTimesByDate(
            final List<Booking> bookings, final ZoneId zoneId, final LocalDate rangeStart, final LocalDate rangeEnd) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        for (final Booking booking : bookings) {
            if (!BookingBlockingStatuses.isDisplayBlocking(booking.getStatus())
                    || booking.getStart() == null
                    || booking.getEnd() == null) {
                continue;
            }
            ZonedDateTime currentZ = booking.getStart()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zoneId)
                    .minusMinutes(BookingBlockingStatuses.BOOKING_CLEARANCE_MINUTES);
            final ZonedDateTime endZ = booking.getEnd()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zoneId)
                    .plusMinutes(BookingBlockingStatuses.BOOKING_CLEARANCE_MINUTES);
            while (currentZ.isBefore(endZ)) {
                final LocalDate currentDate = currentZ.toLocalDate();
                if (!currentDate.isBefore(rangeStart) && !currentDate.isAfter(rangeEnd)) {
                    collectedTimesByDate
                            .computeIfAbsent(currentDate.format(INPUT_DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(currentZ.toLocalTime().format(RESERVATION_TIME_FORMAT));
                }
                currentZ = currentZ.plusMinutes(BookingBlockingStatuses.TIME_SLOT_STEP_MINUTES);
            }
        }
        return collectedTimesByDate;
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collectedTimesByDate,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute < endMinute; minute += BookingBlockingStatuses.TIME_SLOT_STEP_MINUTES) {
            final LocalTime currentTime = LocalTime.ofSecondOfDay((long) minute * 60);
            collectedTimesByDate
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(currentTime.format(RESERVATION_TIME_FORMAT));
        }
    }

    private static Map<String, TreeSet<String>> subtractTimes(
            final Map<String, TreeSet<String>> baseTimesByDate,
            final Map<String, TreeSet<String>> excludedTimesByDate) {
        final Map<String, TreeSet<String>> filteredTimesByDate = new TreeMap<>();
        for (final Map.Entry<String, TreeSet<String>> entry : baseTimesByDate.entrySet()) {
            final TreeSet<String> remainingTimes = new TreeSet<>(entry.getValue());
            remainingTimes.removeAll(excludedTimesByDate.getOrDefault(entry.getKey(), new TreeSet<>()));
            if (!remainingTimes.isEmpty()) {
                filteredTimesByDate.put(entry.getKey(), remainingTimes);
            }
        }
        return filteredTimesByDate;
    }

    private static Map<String, List<String>> toImmutableTimesByDate(final Map<String, TreeSet<String>> timesByDate) {
        final Map<String, List<String>> immutableTimesByDate = new LinkedHashMap<>();
        for (final Map.Entry<String, TreeSet<String>> entry : timesByDate.entrySet()) {
            immutableTimesByDate.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutableTimesByDate);
    }
}
