package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.DisabledTimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DisabledTimeSlotService {

    /**
     * Persists a disabled slot for [start, end) on the given date.
     *
     * @throws SlotHasActiveBookingsException if any PENDING/CONFIRMED booking overlaps the range.
     *         The owner must cancel those bookings before the slot can be disabled.
     * @throws IllegalArgumentException if the range is invalid.
     */
    DisabledTimeSlot disable(int itemId, LocalDate date, LocalTime startTime, LocalTime endTime);

    /** Removes a previously disabled slot. */
    boolean reEnable(int itemId, int disabledSlotId);

    List<DisabledTimeSlot> listAll();

    List<DisabledTimeSlot> listByItem(int itemId);

    List<DisabledTimeSlot> listByItemBetween(int itemId, LocalDate fromDate, LocalDate toDate);
}
