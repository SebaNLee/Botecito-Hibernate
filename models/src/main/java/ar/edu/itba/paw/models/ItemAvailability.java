package ar.edu.itba.paw.models;

import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemAvailability {
    private Integer id;
    private Integer itemId;
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
}
