package ar.edu.itba.paw.models.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** One recurring availability interval for a listing version (local weekday + local times). */
@Getter
@Setter
public class AvailabilityWindow {
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
}
