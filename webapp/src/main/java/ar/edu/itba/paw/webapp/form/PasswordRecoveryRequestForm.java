package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PasswordRecoveryRequestForm {

    @NotBlank(message = "{passwordRecovery.request.validation.email.required}")
    @Email(message = "{passwordRecovery.request.validation.email.invalid}")
    @Size(max = 150, message = "{passwordRecovery.request.validation.email.max}")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
}
