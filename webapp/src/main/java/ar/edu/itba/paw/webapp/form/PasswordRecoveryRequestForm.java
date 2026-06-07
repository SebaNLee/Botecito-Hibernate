package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRecoveryRequestForm {

    @NotBlank(message = "{passwordRecovery.request.validation.email.required}")
    @Email(message = "{passwordRecovery.request.validation.email.invalid}")
    @Size(max = 100, message = "{passwordRecovery.request.validation.email.max}")
    private String email;

    private String sent;

    public void setEmail(final String email) {
        this.email = email == null ? null : email.trim();
    }
}
