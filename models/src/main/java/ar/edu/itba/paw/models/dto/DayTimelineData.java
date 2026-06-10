package ar.edu.itba.paw.models.dto;

import java.time.LocalTime;
import java.util.List;
import lombok.Value;

public record DayTimelineData(
        List<TimeRangeRow> availableRanges, List<TimeRangeRow> bookedRanges, List<SelfBlockRow> selfBlocks) {

    public DayTimelineData {
        availableRanges = List.copyOf(availableRanges);
        bookedRanges = List.copyOf(bookedRanges);
        selfBlocks = List.copyOf(selfBlocks);
    }

    @Value
    public static class TimeRangeRow {
        LocalTime startTime;
        LocalTime endTime;
    }

    @Value
    public static class SelfBlockRow {
        int id;
        LocalTime startTime;
        LocalTime endTime;
    }
}
