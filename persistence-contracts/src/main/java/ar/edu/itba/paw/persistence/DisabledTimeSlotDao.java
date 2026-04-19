package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.DisabledTimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DisabledTimeSlotDao {

    DisabledTimeSlot insert(int itemId, LocalDate date, LocalTime startTime, LocalTime endTime);

    List<DisabledTimeSlot> listAll();

    List<DisabledTimeSlot> listByItem(int itemId);

    List<DisabledTimeSlot> listByItemBetween(int itemId, LocalDate fromDate, LocalDate toDate);

    boolean deleteById(int itemId, int disabledSlotId);

    boolean deleteByRange(int itemId, LocalDate date, LocalTime startTime, LocalTime endTime);
}
