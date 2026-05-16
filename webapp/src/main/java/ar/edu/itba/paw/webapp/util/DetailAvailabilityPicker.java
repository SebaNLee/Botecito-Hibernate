package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds {@link AvailabilityPickerBuilder.Data} for the nuevo item detail pre-booking UI from all
 * {@link AvailabilityWindow} rows for the version (merged) and version-scoped bookings (UTC) converted to the
 * version timezone, including the same 30-minute clearance as {@code BookingHibernateDao}. Date keys and
 * weekday matching use {@code ZonedDateTime.now(listingZone).toLocalDate()} through the picker horizon so the grid
 * matches the listing calendar day, not the app server default zone.
 */
public final class DetailAvailabilityPicker {

    /** Keep in sync with the client date picker anchor range (see {@code date-time-picker.js}). */
    public static final int LISTING_PICKER_MONTHS_AROUND_TODAY = 2;

    private static final int CLEARANCE_MINUTES = 30;

    private static final Set<BookingStatusEnum> BLOCKING_STATUSES = EnumSet.of(
            BookingStatusEnum.PENDING,
            BookingStatusEnum.ACCEPTED,
            BookingStatusEnum.PAID,
            BookingStatusEnum.CONFIRMED);

    private static final DateTimeFormatter INPUT_DATE_FORMAT = AvailabilityPickerBuilder.INPUT_DATE_FORMAT;
    private static final DateTimeFormatter RESERVATION_TIME_FORMAT = AvailabilityPickerBuilder.RESERVATION_TIME_FORMAT;
    private static final int TIME_SLOT_STEP_MINUTES = AvailabilityPickerBuilder.TIME_SLOT_STEP_MINUTES;

    private DetailAvailabilityPicker() {}

    /** Resolves {@code version.timezone} for listing-local calculations; invalid or blank values fall back to UTC. */
    public static ZoneId listingZoneOrUtc(final String versionTimezone) {
        if (versionTimezone == null || versionTimezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(versionTimezone.trim());
        } catch (final RuntimeException ignored) {
            return ZoneOffset.UTC;
        }
    }

    /** Current local date in the listing zone (never the JVM default zone). */
    public static LocalDate listingCalendarToday(final String versionTimezone) {
        return ZonedDateTime.now(listingZoneOrUtc(versionTimezone)).toLocalDate();
    }

    /** Last inclusive calendar day shown in the listing-local picker horizon. */
    public static LocalDate listingCalendarMaxInclusive(final String versionTimezone) {
        return pickerEndDate(listingCalendarToday(versionTimezone));
    }

    public static AvailabilityPickerBuilder.Data build(
            final List<Availability> availabilityWindows,
            final List<Booking> bookings,
            final String versionTimezone) {
        if (availabilityWindows == null || availabilityWindows.isEmpty()) {
            return new AvailabilityPickerBuilder.Data(List.of(), List.of(), Map.of(), Map.of());
        }
        final ZoneId zoneId = listingZoneOrUtc(versionTimezone);
        final LocalDate rangeStart = listingCalendarToday(versionTimezone);
        final LocalDate rangeEnd = listingCalendarMaxInclusive(versionTimezone);
        final Map<String, TreeSet<String>> scheduledTimesByDate = new TreeMap<>();
        for (final Availability window : availabilityWindows) {
            if (!isUsableWindow(window)) {
                continue;
            }
            final DayOfWeek dayOfWeek = window.getWeekday() == null
                    ? null
                    : DayOfWeek.valueOf(window.getWeekday().name());
            mergeOfferedTimesForWindow(
                    scheduledTimesByDate, dayOfWeek, window.getStartTime(), window.getEndTime(), rangeStart, rangeEnd);
        }
        if (scheduledTimesByDate.isEmpty()) {
            return new AvailabilityPickerBuilder.Data(List.of(), List.of(), Map.of(), Map.of());
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
        return new AvailabilityPickerBuilder.Data(
                List.copyOf(offeredDates),
                List.copyOf(occupiedDates),
                toImmutableTimesByDate(availableTimesByDate),
                toImmutableTimesByDate(occupiedTimesByDate));
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
        return rangeStart.plusMonths(LISTING_PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());
    }

    private static Map<String, TreeSet<String>> buildBookedTimesByDate(
            final List<Booking> bookings,
            final ZoneId zoneId,
            final LocalDate rangeStart,
            final LocalDate rangeEnd) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        for (final Booking booking : bookings) {
            if (!isBlocking(booking) || booking.getStart() == null || booking.getEnd() == null) {
                continue;
            }
            ZonedDateTime currentZ = booking.getStart()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zoneId)
                    .minusMinutes(CLEARANCE_MINUTES);
            final ZonedDateTime endZ = booking.getEnd()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zoneId)
                    .plusMinutes(CLEARANCE_MINUTES);
            while (currentZ.isBefore(endZ)) {
                final LocalDate currentDate = currentZ.toLocalDate();
                if (!currentDate.isBefore(rangeStart) && !currentDate.isAfter(rangeEnd)) {
                    collectedTimesByDate
                            .computeIfAbsent(currentDate.format(INPUT_DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(currentZ.toLocalTime().format(RESERVATION_TIME_FORMAT));
                }
                currentZ = currentZ.plusMinutes(TIME_SLOT_STEP_MINUTES);
            }
        }
        return collectedTimesByDate;
    }

    private static boolean isBlocking(final Booking booking) {
        return booking.getStatus() != null && BLOCKING_STATUSES.contains(booking.getStatus());
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collectedTimesByDate,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute < endMinute; minute += TIME_SLOT_STEP_MINUTES) {
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
