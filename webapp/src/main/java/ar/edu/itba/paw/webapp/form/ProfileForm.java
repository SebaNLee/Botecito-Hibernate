package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "{profile.validation.givenName.required}")
    @Size(max = 100, message = "{profile.validation.givenName.max}")
    private String givenName;

    @NotBlank(message = "{profile.validation.lastName.required}")
    @Size(max = 100, message = "{profile.validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{profile.validation.email.required}")
    @Email(message = "{profile.validation.email.invalid}")
    @Size(max = 150, message = "{profile.validation.email.max}")
    private String email;

    @Size(max = 30, message = "{profile.validation.phone.max}")
    private String phone;

    @Size(max = 120, message = "{profile.validation.paymentAlias.max}")
    private String paymentAlias;

    @NotBlank(message = "{profile.validation.preferredLanguage.required}")
    @Pattern(regexp = "es|en", message = "{profile.validation.preferredLanguage.invalid}")
    private String preferredLanguage = "es";

    public void setGivenName(final String givenName) {
        this.givenName = givenName == null ? null : givenName.trim();
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName == null ? null : lastName.trim();
    }

    public void setEmail(final String email) {
        this.email = email == null ? null : email.trim();
    }

    public void setPhone(final String phone) {
        this.phone = phone == null ? null : phone.trim();
    }

    public void setPaymentAlias(final String paymentAlias) {
        this.paymentAlias = paymentAlias == null ? null : paymentAlias.trim();
    }
}
