package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class ReservationRequestForm {
    @NotBlank(message = "{reservation.validation.date.required}")
    private String date;

    @NotBlank(message = "{reservation.validation.startTime.required}")
    private String startTime;

    @NotBlank(message = "{reservation.validation.endTime.required}")
    private String endTime;

    @NotBlank(message = "{reservation.validation.requesterGivenName.required}")
    private String requesterGivenName;

    @NotBlank(message = "{reservation.validation.requesterLastName.required}")
    private String requesterLastName;

    @NotBlank(message = "{reservation.validation.requesterEmail.required}")
    @Email(message = "{reservation.validation.requesterEmail.invalid}")
    private String requesterEmail;

    private String requesterPreferredLanguage = "es";

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

    public String getRequesterGivenName() {
        return requesterGivenName;
    }

    public void setRequesterGivenName(final String requesterGivenName) {
        this.requesterGivenName = requesterGivenName;
    }

    public String getRequesterLastName() {
        return requesterLastName;
    }

    public void setRequesterLastName(final String requesterLastName) {
        this.requesterLastName = requesterLastName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(final String requesterEmail) {
        this.requesterEmail = requesterEmail;
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
