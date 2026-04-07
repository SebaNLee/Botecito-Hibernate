package ar.edu.itba.paw.services.utils;

import java.time.LocalTime;

public class TimeRange {
    private LocalTime startTime;
    private LocalTime endTime;

    public TimeRange(LocalTime start, LocalTime end) {
        setStart(start);
        setEnd(end);
    }

    public LocalTime getStart() {
        return startTime;
    }

    public boolean setStart(LocalTime start) {
        if (endTime != null && start.compareTo(endTime) > 0) return false;
        startTime = start;
        return true;
    }

    public LocalTime getEnd() {
        return endTime;
    }

    public boolean setEnd(LocalTime end) {
        if (startTime != null && startTime.compareTo(end) > 0) return false;
        endTime = end;
        return true;
    }
}
