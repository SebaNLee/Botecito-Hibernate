package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefusePaymentForm {

    @NotBlank(message = "{refusePayment.validation.reason.notBlank}")
    @Size(max = 500, message = "{refusePayment.validation.reason.size}")
    private String reason;
}
