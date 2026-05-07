package ar.edu.itba.paw.webapp.form.nuevo;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetForm {

    @NotBlank(message = "{passwordRecovery.reset.validation.password.required}")
    @Size(min = 8, max = 100, message = "{passwordRecovery.reset.validation.password.size}")
    private String password;

    @NotBlank(message = "{passwordRecovery.reset.validation.confirmPassword.required}")
    private String confirmPassword;

    private String token;
    private String invalid;

    @AssertTrue(message = "{passwordRecovery.reset.validation.password.mismatch}")
    public boolean isPasswordConfirmationValid() {
        if (isBlank(password) || isBlank(confirmPassword)) {
            return true;
        }
        return password.equals(confirmPassword);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
