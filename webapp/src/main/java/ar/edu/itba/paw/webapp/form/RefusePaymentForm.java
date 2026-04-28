package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RefusePaymentForm {

    @NotBlank(message = "{refusePayment.validation.reason.notBlank}")
    @Size(max = 500, message = "{refusePayment.validation.reason.size}")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
