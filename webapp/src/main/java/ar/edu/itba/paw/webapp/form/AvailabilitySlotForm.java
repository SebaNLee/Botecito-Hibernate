package ar.edu.itba.paw.webapp.form;

public class AvailabilitySlotForm {

    private String weekday;
    private String startTime;
    private String endTime;

    public String getWeekday() {
        return weekday;
    }

    public void setWeekday(final String weekday) {
        this.weekday = weekday;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }
}
