package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ReservationRequestForm {
    @NotBlank(message = "{reservation.validation.date.required}")
    private String date;

    @NotBlank(message = "{reservation.validation.startTime.required}")
    private String startTime;

    @NotBlank(message = "{reservation.validation.endTime.required}")
    private String endTime;

    private String requesterPreferredLanguage = "es";

    @Size(max = 1000, message = "{reservation.validation.requestMessage.max}")
    private String requestMessage;

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

    public String getRequesterPreferredLanguage() {
        return requesterPreferredLanguage;
    }

    public void setRequesterPreferredLanguage(final String requesterPreferredLanguage) {
        this.requesterPreferredLanguage = requesterPreferredLanguage;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(final String requestMessage) {
        this.requestMessage = requestMessage;
    }
}
