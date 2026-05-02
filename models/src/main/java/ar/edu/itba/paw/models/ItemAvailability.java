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

    public void setWeekday(final DayOfWeek weekday) {
        this.weekday = weekday;
    }

    public void setWeekday(final String weekday) {
        this.weekday = weekday == null ? null : DayOfWeek.valueOf(weekday);
    }

    public void setStartTime(final LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime == null ? null : LocalTime.parse(startTime);
    }

    public void setEndTime(final LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime == null ? null : LocalTime.parse(endTime);
    }
}
