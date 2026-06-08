package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Setter;

@Setter
public class RegisterForm {

    @NotBlank(message = "{register.validation.givenName.required}")
    @Size(max = 100, message = "{register.validation.givenName.max}")
    private String givenName;

    @NotBlank(message = "{register.validation.lastName.required}")
    @Size(max = 100, message = "{register.validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{register.validation.email.required}")
    @Email(message = "{register.validation.email.invalid}")
    @Size(max = 100, message = "{register.validation.email.max}")
    private String email;

    @NotBlank(message = "{register.validation.password.required}")
    @Size(min = 8, max = 100, message = "{register.validation.password.size}")
    private String password;

    @NotBlank(message = "{register.validation.confirmPassword.required}")
    @Size(max = 100, message = "{register.validation.password.size}")
    private String confirmPassword;

    @Size(max = 30, message = "{register.validation.paymentAlias.max}")
    private String paymentAlias;

    public String getGivenName() {
        return givenName == null ? null : givenName.trim();
    }

    public String getLastName() {
        return lastName == null ? null : lastName.trim();
    }

    public String getEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getPaymentAlias() {
        if (paymentAlias == null) {
            return null;
        }
        final String trimmed = paymentAlias.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @AssertTrue(message = "{register.validation.password.mismatch}")
    public boolean isPasswordConfirmationValid() {
        if (password == null || password.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
