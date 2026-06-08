package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Setter;

@Setter
public class RejectPaymentForm {

    @NotBlank(message = "{requests.booking.rejectPaymentReasonRequired}")
    @Size(max = 255, message = "{requests.booking.rejectPaymentReasonRequired}")
    private String reason;

    public String getReason() {
        if (reason == null) {
            return null;
        }
        final String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}
