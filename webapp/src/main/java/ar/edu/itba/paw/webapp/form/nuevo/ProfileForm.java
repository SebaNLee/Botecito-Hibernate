package ar.edu.itba.paw.webapp.form.nuevo;

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

    @Pattern(regexp = "es|en", message = "{profile.validation.preferredLanguage.invalid}")
    private String preferredLanguage = "es";
}
