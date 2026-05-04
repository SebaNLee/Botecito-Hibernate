package ar.edu.itba.paw.services.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PublishAvailabilityRulesTest {

    @Test
    public void requireValidAvailabilitySlotAcceptsAlignedRange() {
        assertDoesNotThrow(() -> PublishAvailabilityRules.requireValidAvailabilitySlot(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
    }

    @Test
    public void requireValidAvailabilitySlotRejectsOffGrid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PublishAvailabilityRules.requireValidAvailabilitySlot(
                        DayOfWeek.MONDAY, LocalTime.of(9, 15), LocalTime.of(11, 0)));
    }

    @Test
    public void validateOrderedDaySlotsRejectsRangeShorterThanTwoHours() {
        final List<TimeRange> slots = List.of(TimeRange.of(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertEquals(
                PublishAvailabilityRules.DaySlotsIssue.RANGE_TOO_SHORT,
                PublishAvailabilityRules.validateOrderedDaySlots(slots));
    }

    @Test
    public void validateOrderedDaySlotsAcceptsValidTwoRanges() {
        final List<TimeRange> slots = List.of(
                TimeRange.of(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                TimeRange.of(LocalTime.of(14, 0), LocalTime.of(16, 0)));
        assertEquals(
                PublishAvailabilityRules.DaySlotsIssue.OK, PublishAvailabilityRules.validateOrderedDaySlots(slots));
    }
}
