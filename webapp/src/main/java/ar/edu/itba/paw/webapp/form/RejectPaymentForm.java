package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Setter;

@Setter
public class RejectPaymentForm {

    @NotBlank(message = "{requests.booking.rejectPaymentHostMessageRequired}")
    @Size(max = 255, message = "{requests.booking.rejectPaymentHostMessageRequired}")
    private String hostMessage;

    public String getHostMessage() {
        if (hostMessage == null) {
            return null;
        }
        final String trimmed = hostMessage.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}
