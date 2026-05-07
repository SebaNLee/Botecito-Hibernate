package ar.edu.itba.paw.webapp.presentation.nuevo;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@RequiredArgsConstructor
public class AuthPresentation {

    private static final String REGISTER_VIEW = "nuevo/register";
    private static final String PASSWORD_RECOVERY_RESET_VIEW = "nuevo/password-recovery-reset";

    private final UserService userService;
    private final PostRegistrationAuthenticator postRegistrationAuthenticator;

    public ModelAndView login(final LoginForm form) {
        final ModelAndView mav = new ModelAndView("login");
        if (form.getError() != null) {
            mav.addObject("loginError", true);
        }
        if (form.getLogout() != null) {
            mav.addObject("logoutSuccess", true);
        }
        if (form.getRegistered() != null) {
            mav.addObject("registeredSuccess", true);
        }
        if (form.getLegacyToken() != null) {
            mav.addObject("legacyTokenError", true);
        }
        if (form.getPasswordRecovered() != null) {
            mav.addObject("passwordRecoveredSuccess", true);
        }
        return mav;
    }

    public ModelAndView registerForm(final RegisterForm form) {
        return new ModelAndView(REGISTER_VIEW);
    }

    public ModelAndView registerSubmit(final RegisterForm form, final BindingResult errors) {
        if (userService.register(
                        form.getGivenName().trim(),
                        form.getLastName().trim(),
                        form.getEmail().trim(),
                        form.getPassword(),
                        form.getPaymentAlias(),
                        form.getPreferredLanguage())
                != UserService.RegistrationResult.SUCCESS) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView(REGISTER_VIEW);
        }

        if (!postRegistrationAuthenticator.authenticate(
                form.getEmail().trim(), form.getPassword(), form.getRequest())) {
            return new ModelAndView("redirect:/login?registered=true");
        }
        return new ModelAndView("redirect:/");
    }

    public ModelAndView passwordRecoveryRequestForm(final PasswordRecoveryRequestForm form) {
        final ModelAndView mav = new ModelAndView("password-recovery-request");
        if (form.getSent() != null) {
            mav.addObject("recoverySent", true);
        }
        return mav;
    }

    public ModelAndView passwordRecoveryRequestSubmit(final PasswordRecoveryRequestForm form) {
        userService.requestPasswordRecovery(form.getEmail().trim());
        return new ModelAndView("redirect:/password-recovery?sent=true");
    }

    public ModelAndView passwordRecoveryResetForm(final PasswordResetForm form) {
        final ModelAndView mav = new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW);
        mav.addObject("token", form.getToken());
        mav.addObject(
                "tokenValid",
                userService.findByPasswordRecoveryToken(form.getToken()).isPresent());
        if (form.getInvalid() != null) {
            mav.addObject("tokenInvalidError", true);
        }
        return mav;
    }

    public ModelAndView passwordRecoveryResetWithErrors(final PasswordResetForm form) {
        return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                .addObject("token", form.getToken())
                .addObject("tokenValid", true);
    }

    public ModelAndView passwordRecoveryResetSubmit(final PasswordResetForm form) {
        final boolean tokenValid =
                userService.findByPasswordRecoveryToken(form.getToken()).isPresent();
        if (!tokenValid) {
            return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                    .addObject("token", form.getToken())
                    .addObject("tokenValid", false);
        }

        if (userService.resetPassword(form.getToken(), form.getPassword())
                != UserService.PasswordRecoveryResult.SUCCESS) {
            return new ModelAndView("redirect:/password-recovery/" + form.getToken() + "?invalid=true");
        }

        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }
}
