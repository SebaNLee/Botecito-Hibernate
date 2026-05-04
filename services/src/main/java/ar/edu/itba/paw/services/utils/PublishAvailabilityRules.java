package ar.edu.itba.paw.services.utils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared rules for publication weekly availability: used by the publish wizard (web) and by
 * {@code ItemServiceImpl} so the same constraints apply to every entry point.
 */
public final class PublishAvailabilityRules {

    public static final int TIME_STEP_MINUTES = 30;
    public static final long MIN_SLOT_DURATION_MINUTES = 120;
    public static final long MIN_SLOT_SEPARATION_MINUTES = 30;

    public enum SlotTimesIssue {
        OK,
        MISSING_TIMES,
        END_NOT_AFTER_START,
        OFF_TIME_GRID
    }

    public enum DaySlotsIssue {
        OK,
        MISSING_SLOT_OR_TIME,
        END_NOT_AFTER_START,
        OFF_TIME_GRID,
        RANGE_TOO_SHORT,
        RANGES_TOO_CLOSE,
        OVERLAP
    }

    private PublishAvailabilityRules() {}

    public static boolean isOnPublicationTimeGrid(final LocalTime time) {
        return time.getMinute() % TIME_STEP_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    public static SlotTimesIssue validateSlotTimes(final LocalTime start, final LocalTime end) {
        if (start == null || end == null) {
            return SlotTimesIssue.MISSING_TIMES;
        }
        if (!end.isAfter(start)) {
            return SlotTimesIssue.END_NOT_AFTER_START;
        }
        if (!isOnPublicationTimeGrid(start) || !isOnPublicationTimeGrid(end)) {
            return SlotTimesIssue.OFF_TIME_GRID;
        }
        return SlotTimesIssue.OK;
    }

    public static void requireValidAvailabilitySlot(
            final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        if (weekday == null) {
            throw new IllegalArgumentException("availability weekday, start time and end time are required");
        }
        switch (validateSlotTimes(startTime, endTime)) {
            case MISSING_TIMES -> throw new IllegalArgumentException(
                    "availability weekday, start time and end time are required");
            case END_NOT_AFTER_START -> throw new IllegalArgumentException(
                    "availability end time must be after start time");
            case OFF_TIME_GRID -> throw new IllegalArgumentException("availability times must use 30 minute steps");
            case OK -> {
                // valid
            }
        }
    }

    /**
     * Validates slots for one weekday in wizard order (separation), then overlap after sorting by start.
     */
    public static DaySlotsIssue validateOrderedDaySlots(final List<TimeRange> daySlots) {
        if (daySlots == null || daySlots.isEmpty()) {
            return DaySlotsIssue.MISSING_SLOT_OR_TIME;
        }
        final List<LocalTime[]> parsed = new ArrayList<>();
        LocalTime previousEnd = null;
        for (final TimeRange slot : daySlots) {
            if (slot == null) {
                return DaySlotsIssue.MISSING_SLOT_OR_TIME;
            }
            final SlotTimesIssue shape = validateSlotTimes(slot.getStart(), slot.getEnd());
            switch (shape) {
                case MISSING_TIMES -> {
                    return DaySlotsIssue.MISSING_SLOT_OR_TIME;
                }
                case END_NOT_AFTER_START -> {
                    return DaySlotsIssue.END_NOT_AFTER_START;
                }
                case OFF_TIME_GRID -> {
                    return DaySlotsIssue.OFF_TIME_GRID;
                }
                case OK -> {
                    // continue
                }
            }
            final LocalTime start = slot.getStart();
            final LocalTime end = slot.getEnd();
            if (Duration.between(start, end).toMinutes() < MIN_SLOT_DURATION_MINUTES) {
                return DaySlotsIssue.RANGE_TOO_SHORT;
            }
            if (previousEnd != null && Duration.between(previousEnd, start).toMinutes() < MIN_SLOT_SEPARATION_MINUTES) {
                return DaySlotsIssue.RANGES_TOO_CLOSE;
            }
            previousEnd = end;
            parsed.add(new LocalTime[] {start, end});
        }
        parsed.sort(Comparator.comparing(r -> r[0]));
        for (int i = 1; i < parsed.size(); i++) {
            if (parsed.get(i)[0].isBefore(parsed.get(i - 1)[1])) {
                return DaySlotsIssue.OVERLAP;
            }
        }
        return DaySlotsIssue.OK;
    }
}
