package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.LoginForm;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import org.springframework.web.servlet.ModelAndView;

public final class AuthPresentation {

    private static final String REGISTER_VIEW = "register";
    private static final String PASSWORD_RECOVERY_RESET_VIEW = "password-recovery-reset";

    private AuthPresentation() {}

    public static ModelAndView login(final LoginForm form) {
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
        if (form.getVerificationSent() != null) {
            mav.addObject("verificationSentSuccess", true);
        }
        if (form.getVerified() != null) {
            mav.addObject("emailVerifiedSuccess", true);
        }
        if (form.getVerificationInvalid() != null) {
            mav.addObject("verificationInvalidError", true);
        }
        if (form.getUnverified() != null) {
            mav.addObject("unverifiedError", true);
        }
        if (form.getLegacyToken() != null) {
            mav.addObject("legacyTokenError", true);
        }
        if (form.getPasswordRecovered() != null) {
            mav.addObject("passwordRecoveredSuccess", true);
        }
        if (form.getSessionExpired() != null) {
            mav.addObject("sessionExpiredWarning", true);
        }
        return mav;
    }

    public static ModelAndView registerForm(final RegisterForm form) {
        return new ModelAndView(REGISTER_VIEW);
    }

    public static ModelAndView registerSuccessRedirect() {
        return new ModelAndView("redirect:/login?verificationSent=true");
    }

    public static ModelAndView passwordRecoveryRequestForm(final PasswordRecoveryRequestForm form) {
        final ModelAndView mav = new ModelAndView("password-recovery-request");
        if (form.getSent() != null) {
            mav.addObject("recoverySent", true);
        }
        return mav;
    }

    public static ModelAndView passwordRecoveryRequestSuccessRedirect() {
        return new ModelAndView("redirect:/password-recovery?sent=true");
    }

    public static ModelAndView passwordRecoveryResetInvalidTokenRedirect(final String token) {
        return new ModelAndView("redirect:/password-recovery/" + token + "?invalid=true");
    }

    public static ModelAndView passwordRecoveryResetForm(final String token) {
        return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                .addObject("token", token)
                .addObject("tokenValid", true);
    }

    public static ModelAndView passwordRecoveryResetInvalidTokenView(final String token) {
        return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                .addObject("token", token)
                .addObject("tokenValid", false);
    }

    public static ModelAndView passwordRecoveryResetSuccessRedirect() {
        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    public static ModelAndView verifyEmailSuccessRedirect() {
        return new ModelAndView("redirect:/");
    }

    public static ModelAndView verifyEmailInvalidRedirect() {
        return new ModelAndView("redirect:/login?verificationInvalid=true");
    }
}
