package ar.edu.itba.paw.services.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public final class AvailabilityPickerData {
    private final List<String> offeredDates;
    private final List<String> occupiedDates;
    private final Map<String, List<String>> offeredTimesByDate;
    private final Map<String, List<String>> occupiedTimesByDate;

    public AvailabilityPickerData(
            final List<String> offeredDates,
            final List<String> occupiedDates,
            final Map<String, List<String>> offeredTimesByDate,
            final Map<String, List<String>> occupiedTimesByDate) {
        this.offeredDates = offeredDates == null ? List.of() : List.copyOf(offeredDates);
        this.occupiedDates = occupiedDates == null ? List.of() : List.copyOf(occupiedDates);
        this.offeredTimesByDate = offeredTimesByDate == null ? Map.of() : Map.copyOf(offeredTimesByDate);
        this.occupiedTimesByDate = occupiedTimesByDate == null ? Map.of() : Map.copyOf(occupiedTimesByDate);
    }
}
