package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
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
    @Size(max = 150, message = "{register.validation.email.max}")
    private String email;

    @NotBlank(message = "{register.validation.password.required}")
    @Size(min = 8, max = 100, message = "{register.validation.password.size}")
    private String password;

    @NotBlank(message = "{register.validation.confirmPassword.required}")
    private String confirmPassword;

    @Size(max = 120, message = "{register.validation.paymentAlias.max}")
    private String paymentAlias;

    private String preferredLanguage;

    @AssertTrue(message = "{register.validation.password.mismatch}")
    public boolean isPasswordConfirmationValid() {
        if (password == null || password.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            return true;
        }
        return password.equals(confirmPassword);
    }

    public void setGivenName(final String givenName) {
        this.givenName = givenName == null ? null : givenName.trim();
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName == null ? null : lastName.trim();
    }

    public void setEmail(final String email) {
        this.email = email == null ? null : email.trim();
    }

    public void setPaymentAlias(final String paymentAlias) {
        this.paymentAlias = paymentAlias == null ? null : paymentAlias.trim();
    }
}
