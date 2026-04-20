package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class MarketplaceAvailabilityMatcher {
    private static final int TIME_SLOT_STEP_MINUTES = 30;
    private static final int MIN_BOOKING_DURATION_MINUTES = 120;

    private MarketplaceAvailabilityMatcher() {}

    static boolean matches(
            final ItemSearchCriteria criteria,
            final List<ItemAvailability> availabilities,
            final List<ItemBooking> bookings) {
        if (isBlank(criteria.getDate())) {
            return matchesWithoutCalendarDate(criteria, availabilities);
        }

        final TreeSet<String> availableTimes = availableTimesForDate(criteria.getDate(), availabilities, bookings);
        if (availableTimes.isEmpty()) {
            return false;
        }

        if (isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return hasAnyContinuousTwoHourWindow(List.copyOf(availableTimes));
        }

        if (!isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return hasContinuousTwoHourWindowStartingAt(List.copyOf(availableTimes), criteria.getStartTime());
        }

        if (isBlank(criteria.getStartTime())) {
            return hasContinuousTwoHourWindowEndingAt(List.copyOf(availableTimes), criteria.getEndTime());
        }

        return hasContinuousAvailability(List.copyOf(availableTimes), criteria.getStartTime(), criteria.getEndTime());
    }

    /**
     * When no calendar date is chosen, match against recurring weekly availability only
     * (bookings are date-specific and are not applied for this mode).
     */
    private static boolean matchesWithoutCalendarDate(
            final ItemSearchCriteria criteria, final List<ItemAvailability> availabilities) {
        if (isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return true;
        }

        if (!isBlank(criteria.getStartTime()) && isBlank(criteria.getEndTime())) {
            return matchesStartTimeAnyWeekday(criteria.getStartTime(), availabilities);
        }

        if (isBlank(criteria.getStartTime())) {
            return matchesEndTimeAnyWeekday(criteria.getEndTime(), availabilities);
        }

        return matchesRangeAnyWeekday(criteria.getStartTime(), criteria.getEndTime(), availabilities);
    }

    private static boolean matchesStartTimeAnyWeekday(
            final String requestedStartTime, final List<ItemAvailability> availabilities) {
        for (final DayOfWeek dow : DayOfWeek.values()) {
            final TreeSet<String> times = availableTimesForWeekday(dow, availabilities);
            if (hasContinuousTwoHourWindowStartingAt(List.copyOf(times), requestedStartTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEndTimeAnyWeekday(
            final String requestedEndTime, final List<ItemAvailability> availabilities) {
        for (final DayOfWeek dow : DayOfWeek.values()) {
            final TreeSet<String> times = availableTimesForWeekday(dow, availabilities);
            if (hasContinuousTwoHourWindowEndingAt(List.copyOf(times), requestedEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRangeAnyWeekday(
            final String requestedStartTime,
            final String requestedEndTime,
            final List<ItemAvailability> availabilities) {
        for (final DayOfWeek dow : DayOfWeek.values()) {
            final TreeSet<String> times = availableTimesForWeekday(dow, availabilities);
            if (hasContinuousAvailability(List.copyOf(times), requestedStartTime, requestedEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static TreeSet<String> availableTimesForWeekday(
            final DayOfWeek dow, final List<ItemAvailability> availabilities) {
        final TreeSet<String> scheduledTimes = new TreeSet<>();
        for (final ItemAvailability availability : availabilities) {
            if (availability.getWeekday() == dow) {
                addTimeRange(scheduledTimes, availability.getStartTime(), availability.getEndTime());
            }
        }
        return scheduledTimes;
    }

    private static TreeSet<String> availableTimesForDate(
            final String requestedDate, final List<ItemAvailability> availabilities, final List<ItemBooking> bookings) {
        try {
            final LocalDate date = LocalDate.parse(requestedDate);
            final TreeSet<String> scheduledTimes = new TreeSet<>();
            final TreeSet<String> bookedTimes = new TreeSet<>();
            for (final ItemAvailability availability : availabilities) {
                if (availability.getWeekday() == date.getDayOfWeek()) {
                    addTimeRange(scheduledTimes, availability.getStartTime(), availability.getEndTime());
                }
            }
            for (final ItemBooking booking : bookings) {
                addBookedTimesForDate(bookedTimes, date, booking);
            }
            scheduledTimes.removeAll(bookedTimes);
            return scheduledTimes;
        } catch (final DateTimeParseException exception) {
            return new TreeSet<>();
        }
    }

    private static void addTimeRange(final TreeSet<String> times, final LocalTime startTime, final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute <= endMinute; minute += TIME_SLOT_STEP_MINUTES) {
            times.add(LocalTime.ofSecondOfDay((long) minute * 60).toString());
        }
    }

    private static void addBookedTimesForDate(
            final TreeSet<String> bookedTimes, final LocalDate date, final ItemBooking booking) {
        if (!isBlockingBooking(booking)) {
            return;
        }
        OffsetDateTime currentTime = booking.getStartTime();
        final OffsetDateTime endTime = booking.getEndTime();
        while (currentTime.isBefore(endTime)) {
            if (date.equals(currentTime.toLocalDate())) {
                bookedTimes.add(currentTime.toLocalTime().toString());
            }
            currentTime = currentTime.plusMinutes(TIME_SLOT_STEP_MINUTES);
        }
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == null
                || booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED;
    }

    private static boolean hasAnyContinuousTwoHourWindow(final List<String> availableTimes) {
        for (int startIndex = 0; startIndex < availableTimes.size(); startIndex++) {
            for (int endIndex = startIndex + 1; endIndex < availableTimes.size(); endIndex++) {
                if (hasContinuousAvailability(
                        availableTimes, availableTimes.get(startIndex), availableTimes.get(endIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowStartingAt(
            final List<String> availableTimes, final String requestedStartTime) {
        if (!availableTimes.contains(requestedStartTime)) {
            return false;
        }
        for (final String possibleEndTime : availableTimes) {
            if (hasContinuousAvailability(availableTimes, requestedStartTime, possibleEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowEndingAt(
            final List<String> availableTimes, final String requestedEndTime) {
        if (!availableTimes.contains(requestedEndTime)) {
            return false;
        }
        for (final String possibleStartTime : availableTimes) {
            if (hasContinuousAvailability(availableTimes, possibleStartTime, requestedEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousAvailability(
            final List<String> availableTimes, final String requestedStartTime, final String requestedEndTime) {
        try {
            final Set<String> availableTimeSet = Set.copyOf(availableTimes);
            final LocalTime startTime = LocalTime.parse(requestedStartTime);
            final LocalTime endTime = LocalTime.parse(requestedEndTime);
            if (!endTime.isAfter(startTime)
                    || Duration.between(startTime, endTime).toMinutes() < MIN_BOOKING_DURATION_MINUTES) {
                return false;
            }
            for (int minute = startTime.toSecondOfDay() / 60;
                    minute <= endTime.toSecondOfDay() / 60;
                    minute += TIME_SLOT_STEP_MINUTES) {
                if (!availableTimeSet.contains(
                        LocalTime.ofSecondOfDay((long) minute * 60).toString())) {
                    return false;
                }
            }
            return true;
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
