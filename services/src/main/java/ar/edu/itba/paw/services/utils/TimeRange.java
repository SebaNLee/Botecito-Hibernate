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

    public boolean intersects(TimeRange other) {
        // this start < other start < this end
        boolean case1 = this.startTime.compareTo(other.startTime) < 0 && other.startTime.compareTo(this.endTime) < 0;
        // other start < this start < other end
        boolean case2 = other.startTime.compareTo(this.startTime) < 0 && this.startTime.compareTo(other.endTime) < 0;

        return case1 || case2;
    }

    private void throwException() {
        throw new IllegalArgumentException("End time must be greater than start time");
    }
}
