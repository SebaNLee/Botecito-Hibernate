package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class BlockSlotForm {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String date;

    @NotBlank
    @Pattern(regexp = "\\d{2}:\\d{2}")
    private String startTime;

    @NotBlank
    @Pattern(regexp = "\\d{2}:\\d{2}")
    private String endTime;

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
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
