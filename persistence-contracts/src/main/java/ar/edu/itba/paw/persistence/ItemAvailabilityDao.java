package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ItemAvailability;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ItemAvailabilityDao {

    ItemAvailability createItemAvailability(int itemId, String weekday, String startTime, String endTime);

    List<ItemAvailability> listAvailabilities();

    List<ItemAvailability> listAvailabilitiesByItemId(int itemId);

    Optional<ItemAvailability> findNextAvailabilityByItemId(int itemId);

    Integer insertAvailability(int itemId, DayOfWeek weekday, LocalTime startTime, LocalTime endTime);
}
