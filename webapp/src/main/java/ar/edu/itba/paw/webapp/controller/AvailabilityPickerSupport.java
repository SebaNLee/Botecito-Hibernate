package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.web.servlet.ModelAndView;

final class AvailabilityPickerSupport {
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESERVATION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int TIME_SLOT_STEP_MINUTES = 30;
    private static final int MIN_BOOKING_DURATION_MINUTES = 120;
    private static final int PICKER_MONTHS_AROUND_TODAY = 2;

    private AvailabilityPickerSupport() {}

    static void addAvailabilityPickerData(
            final ModelAndView mav, final String prefix, final AvailabilityPickerData availabilityData) {
        mav.addObject(prefix + "OfferedDatesJson", toJsonArray(availabilityData.getOfferedDates()));
        mav.addObject(prefix + "OccupiedDatesJson", toJsonArray(availabilityData.getOccupiedDates()));
        mav.addObject(prefix + "OfferedTimesJson", toJsonMap(availabilityData.getOfferedTimesByDate()));
        mav.addObject(prefix + "OccupiedTimesJson", toJsonMap(availabilityData.getOccupiedTimesByDate()));
    }

    static AvailabilityPickerData buildAvailabilityPickerData(
            final List<ItemAvailability> availabilities, final List<ItemBooking> bookings) {
        final Set<Integer> itemIds = new LinkedHashSet<>();
        final Map<Integer, List<ItemAvailability>> availabilitiesByItemId = new LinkedHashMap<>();
        final Map<Integer, List<ItemBooking>> bookingsByItemId = new LinkedHashMap<>();
        final Map<String, TreeSet<String>> scheduledTimesByDate = new TreeMap<>();
        final Map<String, TreeSet<String>> availableTimesByDate = new TreeMap<>();

        for (final ItemAvailability availability : availabilities) {
            itemIds.add(availability.getItemId());
            availabilitiesByItemId
                    .computeIfAbsent(availability.getItemId(), ignored -> new ArrayList<>())
                    .add(availability);
        }

        for (final ItemBooking booking : bookings) {
            itemIds.add(booking.getItemId());
            bookingsByItemId
                    .computeIfAbsent(booking.getItemId(), ignored -> new ArrayList<>())
                    .add(booking);
        }

        for (final Integer itemId : itemIds) {
            final Map<String, TreeSet<String>> itemScheduledTimesByDate =
                    buildScheduledTimesByDate(availabilitiesByItemId.getOrDefault(itemId, List.of()));
            final Map<String, TreeSet<String>> itemBookedTimesByDate =
                    buildBookedTimesByDate(bookingsByItemId.getOrDefault(itemId, List.of()));
            final Map<String, TreeSet<String>> itemAvailableTimesByDate =
                    subtractTimes(itemScheduledTimesByDate, itemBookedTimesByDate);

            mergeTimesByDate(scheduledTimesByDate, itemScheduledTimesByDate);
            mergeTimesByDate(availableTimesByDate, itemAvailableTimesByDate);
        }

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

        return new AvailabilityPickerData(
                List.copyOf(offeredDates),
                List.copyOf(occupiedDates),
                toImmutableTimesByDate(availableTimesByDate),
                toImmutableTimesByDate(occupiedTimesByDate));
    }

    static String resolveSelectedDate(
            final String requestedDate, final List<String> offeredDates, final String fallbackDate) {
        if (requestedDate != null && offeredDates.contains(requestedDate)) {
            return requestedDate;
        }
        return fallbackDate;
    }

    static String resolveSelectedTime(
            final String requestedTime, final List<String> offeredTimes, final String fallbackTime) {
        if (requestedTime != null && offeredTimes.contains(requestedTime)) {
            return requestedTime;
        }
        return fallbackTime;
    }

    static boolean hasContinuousAvailability(
            final List<String> offeredTimes, final String requestedStartTime, final String requestedEndTime) {
        try {
            final Set<String> offeredTimeSet = Set.copyOf(offeredTimes);
            final LocalTime startTime = LocalTime.parse(requestedStartTime);
            final LocalTime endTime = LocalTime.parse(requestedEndTime);

            if (!endTime.isAfter(startTime)) {
                return false;
            }

            if (Duration.between(startTime, endTime).toMinutes() < MIN_BOOKING_DURATION_MINUTES) {
                return false;
            }

            for (LocalTime currentTime = startTime;
                    !currentTime.isAfter(endTime);
                    currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES)) {
                if (!offeredTimeSet.contains(currentTime.format(RESERVATION_TIME_FORMAT))) {
                    return false;
                }
            }

            return true;
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static Map<String, TreeSet<String>> buildScheduledTimesByDate(final List<ItemAvailability> availabilities) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        final LocalDate startDate = availabilityStartDate();
        final LocalDate endDate = pickerEndDate();

        for (final ItemAvailability availability : availabilities) {
            final DayOfWeek weekday = availability.getWeekday();
            final LocalTime startTime = availability.getStartTime();
            final LocalTime endTime = availability.getEndTime();

            for (LocalDate currentDate = startDate;
                    !currentDate.isAfter(endDate);
                    currentDate = currentDate.plusDays(1)) {
                if (currentDate.getDayOfWeek() != weekday) {
                    continue;
                }

                addTimeRange(collectedTimesByDate, currentDate.format(INPUT_DATE_FORMAT), startTime, endTime);
            }
        }

        return collectedTimesByDate;
    }

    private static Map<String, TreeSet<String>> buildBookedTimesByDate(final List<ItemBooking> bookings) {
        final Map<String, TreeSet<String>> collectedTimesByDate = new TreeMap<>();
        final LocalDate startDate = availabilityStartDate();
        final LocalDate endDate = pickerEndDate();

        for (final ItemBooking booking : bookings) {
            OffsetDateTime currentTime = booking.getStartTime();
            final OffsetDateTime endTime = booking.getEndTime();

            while (currentTime.isBefore(endTime)) {
                final LocalDate currentDate = currentTime.toLocalDate();
                if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                    collectedTimesByDate
                            .computeIfAbsent(currentDate.format(INPUT_DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(currentTime.format(RESERVATION_TIME_FORMAT));
                }
                currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES);
            }
        }

        return collectedTimesByDate;
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collectedTimesByDate,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        for (LocalTime currentTime = startTime;
                !currentTime.isAfter(endTime);
                currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES)) {
            collectedTimesByDate
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(currentTime.format(RESERVATION_TIME_FORMAT));
        }
    }

    private static void mergeTimesByDate(
            final Map<String, TreeSet<String>> target, final Map<String, TreeSet<String>> source) {
        for (final Map.Entry<String, TreeSet<String>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), ignored -> new TreeSet<>()).addAll(entry.getValue());
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
        return immutableTimesByDate;
    }

    private static LocalDate availabilityStartDate() {
        return LocalDate.now();
    }

    private static LocalDate pickerEndDate() {
        return LocalDate.now().plusMonths(PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());
    }

    private static String toJsonArray(final List<String> values) {
        final StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escapeJson(values.get(index))).append('"');
        }
        return json.append(']').toString();
    }

    private static String toJsonMap(final Map<String, List<String>> values) {
        final List<String> entries = new ArrayList<>();
        for (final Map.Entry<String, List<String>> entry : values.entrySet()) {
            entries.add("\"" + escapeJson(entry.getKey()) + "\":" + toJsonArray(entry.getValue()));
        }
        return "{" + String.join(",", entries) + "}";
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static final class AvailabilityPickerData {
        private final List<String> offeredDates;
        private final List<String> occupiedDates;
        private final Map<String, List<String>> offeredTimesByDate;
        private final Map<String, List<String>> occupiedTimesByDate;

        private AvailabilityPickerData(
                final List<String> offeredDates,
                final List<String> occupiedDates,
                final Map<String, List<String>> offeredTimesByDate,
                final Map<String, List<String>> occupiedTimesByDate) {
            this.offeredDates = offeredDates;
            this.occupiedDates = occupiedDates;
            this.offeredTimesByDate = offeredTimesByDate;
            this.occupiedTimesByDate = occupiedTimesByDate;
        }

        public List<String> getOfferedDates() {
            return offeredDates;
        }

        public List<String> getOccupiedDates() {
            return occupiedDates;
        }

        public Map<String, List<String>> getOfferedTimesByDate() {
            return offeredTimesByDate;
        }

        public Map<String, List<String>> getOccupiedTimesByDate() {
            return occupiedTimesByDate;
        }
    }
}
