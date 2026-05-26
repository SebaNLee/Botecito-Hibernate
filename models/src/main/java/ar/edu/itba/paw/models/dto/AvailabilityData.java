package ar.edu.itba.paw.models.dto;

import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class AvailabilityData {
    List<String> offeredDates;
    List<String> occupiedDates;
    Map<String, List<String>> offeredTimesByDate;
    Map<String, List<String>> occupiedTimesByDate;
}
