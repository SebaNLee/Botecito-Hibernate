package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.Users;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Setter;

@Setter
public class SettingsForm {

    @NotBlank(message = "{settings.validation.givenName.required}")
    @Size(max = 100, message = "{settings.validation.givenName.max}")
    private String givenName;

    @NotBlank(message = "{settings.validation.lastName.required}")
    @Size(max = 100, message = "{settings.validation.lastName.max}")
    private String lastName;

    @NotBlank(message = "{settings.validation.email.required}")
    @Email(message = "{settings.validation.email.invalid}")
    @Size(max = 100, message = "{settings.validation.email.max}")
    private String email;

    @Size(max = 30, message = "{settings.validation.phone.max}")
    private String phone;

    @Size(max = 30, message = "{settings.validation.paymentAlias.max}")
    private String paymentAlias;

    @NotBlank(message = "{settings.validation.preferredLanguage.required}")
    @Pattern(regexp = "es|en", message = "{settings.validation.preferredLanguage.invalid}")
    private String preferredLanguage = "es";

    public String getGivenName() {
        return givenName == null ? null : givenName.trim();
    }

    public String getLastName() {
        return lastName == null ? null : lastName.trim();
    }

    public String getEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String getPhone() {
        if (phone == null) {
            return null;
        }
        final String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getPaymentAlias() {
        if (paymentAlias == null) {
            return null;
        }
        final String trimmed = paymentAlias.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public static SettingsForm fromUser(final Users user) {
        final SettingsForm form = new SettingsForm();
        form.setGivenName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setPaymentAlias(user.getAlias());
        form.setPreferredLanguage(
                PreferredLanguageModel.fromPersistence(user.getLanguage()).getPersistenceCode());
        return form;
    }
}
