package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
