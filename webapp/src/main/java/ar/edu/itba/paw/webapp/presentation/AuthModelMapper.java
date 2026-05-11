package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import org.springframework.stereotype.Component;

@Component
public class AuthModelMapper {

    public UserModel fromRegisterForm(final RegisterForm form) {
        final UserModel user = new UserModel();
        user.setGivenName(trim(form.getGivenName()));
        user.setLastName(trim(form.getLastName()));
        user.setEmail(trim(form.getEmail()));
        user.setPaymentAlias(form.getPaymentAlias());
        user.setPreferredLanguage(PreferredLanguageModel.fromInput(form.getPreferredLanguage()));
        return user;
    }

    public UserModel fromPasswordRecoveryRequestForm(final PasswordRecoveryRequestForm form) {
        final UserModel user = new UserModel();
        user.setEmail(trim(form.getEmail()));
        return user;
    }

    public UserModel fromPasswordResetForm(final PasswordResetForm form) {
        final UserModel user = new UserModel();
        user.setPasswordRecoveryToken(trim(form.getToken()));
        return user;
    }

    private static String trim(final String value) {
        return value == null ? null : value.trim();
    }
}
