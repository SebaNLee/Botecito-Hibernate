package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

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
