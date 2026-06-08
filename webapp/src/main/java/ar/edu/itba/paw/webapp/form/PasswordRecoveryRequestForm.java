package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Setter;

@Setter
public class PasswordRecoveryRequestForm {

    @NotBlank(message = "{passwordRecovery.request.validation.email.required}")
    @Email(message = "{passwordRecovery.request.validation.email.invalid}")
    @Size(max = 100, message = "{passwordRecovery.request.validation.email.max}")
    private String email;

    private String sent;

    public String getEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String getSent() {
        return sent;
    }
}
