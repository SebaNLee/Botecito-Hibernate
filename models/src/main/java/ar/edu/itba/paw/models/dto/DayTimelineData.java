package ar.edu.itba.paw.models.dto;

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
        String startTime;
        String endTime;
    }

    @Value
    public static class SelfBlockRow {
        int id;
        String startTime;
        String endTime;
    }
}
