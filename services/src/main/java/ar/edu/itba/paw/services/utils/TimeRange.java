package ar.edu.itba.paw.services.utils;

import java.time.LocalTime;

public class TimeRange {
    private LocalTime startTime;
    private LocalTime endTime;

    public static TimeRange of(LocalTime start, LocalTime end) {
        TimeRange tr = new TimeRange();
        tr.setStart(start);
        tr.setEnd(end);
        return tr;
    }

    public LocalTime getStart() {
        return startTime;
    }

    public void setStart(LocalTime start) {
        if (endTime != null && start.compareTo(endTime) > 0) throwException();
        startTime = start;
    }

    public LocalTime getEnd() {
        return endTime;
    }

    public void setEnd(LocalTime end) {
        if (startTime != null && startTime.compareTo(end) > 0) throwException();
        endTime = end;
    }

    private void throwException() {
        throw new IllegalArgumentException("End time must be greater than start time");
    }
}
