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

    @Override
    public AvailabilityData buildAvailabilityData(
            final List<Availability> availabilityWindows, final List<Booking> bookings, final String versionTimezone) {
        if (availabilityWindows == null || availabilityWindows.isEmpty()) {
            return emptyData();
        }
        final ZoneId zoneId = listingZoneOrUtc(versionTimezone);
        final LocalDate rangeStart = listingCalendarToday(versionTimezone);
        final LocalDate rangeEnd = listingCalendarMaxInclusive(versionTimezone);
        final Map<LocalDate, TreeSet<LocalTime>> scheduledTimesByDate = new TreeMap<>();
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
        final Map<LocalDate, TreeSet<LocalTime>> bookedTimesByDate =
                buildBookedTimesByDate(bookings == null ? List.of() : bookings, zoneId, rangeStart, rangeEnd);
        final Map<LocalDate, TreeSet<LocalTime>> availableTimesByDate =
                subtractTimes(scheduledTimesByDate, bookedTimesByDate);
        final Map<LocalDate, TreeSet<LocalTime>> occupiedTimesByDate =
                subtractTimes(scheduledTimesByDate, availableTimesByDate);

        final List<LocalDate> offeredDates = new ArrayList<>();
        final List<LocalDate> occupiedDates = new ArrayList<>();
        for (final Map.Entry<LocalDate, TreeSet<LocalTime>> scheduledTimesEntry : scheduledTimesByDate.entrySet()) {
            final LocalDate date = scheduledTimesEntry.getKey();
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
                availableRanges.add(
                        new DayTimelineData.TimeRangeRow(availability.getStartTime(), availability.getEndTime()));
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
                                .toLocalTime(),
                        booking.getEnd()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()));
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
                                .toLocalTime(),
                        block.getEnd()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zone)
                                .toLocalTime()));
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
            final Map<LocalDate, TreeSet<LocalTime>> collectedTimesByDate,
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
            addTimeRange(collectedTimesByDate, currentDate, startTime, endTime);
        }
    }

    private static LocalDate pickerEndDate(final LocalDate rangeStart) {
        return rangeStart.plusDays(BookingBlockingStatuses.LISTING_PICKER_DAYS_AHEAD);
    }

    private static Map<LocalDate, TreeSet<LocalTime>> buildBookedTimesByDate(
            final List<Booking> bookings, final ZoneId zoneId, final LocalDate rangeStart, final LocalDate rangeEnd) {
        final Map<LocalDate, TreeSet<LocalTime>> collectedTimesByDate = new TreeMap<>();
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
                            .computeIfAbsent(currentDate, ignored -> new TreeSet<>())
                            .add(currentZ.toLocalTime());
                }
                currentZ = currentZ.plusMinutes(BookingBlockingStatuses.TIME_SLOT_STEP_MINUTES);
            }
        }
        return collectedTimesByDate;
    }

    private static void addTimeRange(
            final Map<LocalDate, TreeSet<LocalTime>> collectedTimesByDate,
            final LocalDate date,
            final LocalTime startTime,
            final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute < endMinute; minute += BookingBlockingStatuses.TIME_SLOT_STEP_MINUTES) {
            final LocalTime currentTime = LocalTime.ofSecondOfDay((long) minute * 60);
            collectedTimesByDate
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(currentTime);
        }
    }

    private static Map<LocalDate, TreeSet<LocalTime>> subtractTimes(
            final Map<LocalDate, TreeSet<LocalTime>> baseTimesByDate,
            final Map<LocalDate, TreeSet<LocalTime>> excludedTimesByDate) {
        final Map<LocalDate, TreeSet<LocalTime>> filteredTimesByDate = new TreeMap<>();
        for (final Map.Entry<LocalDate, TreeSet<LocalTime>> entry : baseTimesByDate.entrySet()) {
            final TreeSet<LocalTime> remainingTimes = new TreeSet<>(entry.getValue());
            remainingTimes.removeAll(excludedTimesByDate.getOrDefault(entry.getKey(), new TreeSet<>()));
            if (!remainingTimes.isEmpty()) {
                filteredTimesByDate.put(entry.getKey(), remainingTimes);
            }
        }
        return filteredTimesByDate;
    }

    private static Map<LocalDate, List<LocalTime>> toImmutableTimesByDate(
            final Map<LocalDate, TreeSet<LocalTime>> timesByDate) {
        final Map<LocalDate, List<LocalTime>> immutableTimesByDate = new LinkedHashMap<>();
        for (final Map.Entry<LocalDate, TreeSet<LocalTime>> entry : timesByDate.entrySet()) {
            immutableTimesByDate.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutableTimesByDate);
    }
}
