package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.webapp.form.nuevo.ProfileForm;

public class ProfileModelMapper {

    public UserModel fromProfileForm(final ProfileForm form, final Integer currentUserId) {
        final UserModel user = new UserModel();
        user.setId(currentUserId);
        user.setGivenName(trim(form.getGivenName()));
        user.setLastName(trim(form.getLastName()));
        user.setEmail(trim(form.getEmail()));
        user.setPhone(form.getPhone());
        user.setPaymentAlias(form.getPaymentAlias());
        user.setPreferredLanguage(PreferredLanguageModel.fromInput(form.getPreferredLanguage()));
        return user;
    }

    public void populateProfileForm(final ProfileForm form, final UserModel user) {
        if (form == null || user == null || form.getEmail() != null) {
            return;
        }
        form.setGivenName(user.getGivenName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setPaymentAlias(user.getPaymentAlias());
        form.setPreferredLanguage(
                user.getPreferredLanguage() == null
                        ? null
                        : user.getPreferredLanguage().getPersistenceCode());
    }

    private static String trim(final String value) {
        return value == null ? null : value.trim();
    }
}
