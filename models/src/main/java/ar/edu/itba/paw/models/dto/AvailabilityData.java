package ar.edu.itba.paw.models.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class AvailabilityData {
    List<LocalDate> offeredDates;
    List<LocalDate> occupiedDates;
    Map<LocalDate, List<LocalTime>> offeredTimesByDate;
    Map<LocalDate, List<LocalTime>> occupiedTimesByDate;
}
