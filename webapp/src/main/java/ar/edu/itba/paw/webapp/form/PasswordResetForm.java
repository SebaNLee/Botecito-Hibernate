package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PasswordResetForm {

    @NotBlank(message = "{passwordRecovery.reset.validation.password.required}")
    @Size(min = 8, max = 100, message = "{passwordRecovery.reset.validation.password.size}")
    private String password;

    @NotBlank(message = "{passwordRecovery.reset.validation.confirmPassword.required}")
    private String confirmPassword;

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
