package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.LoginForm;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class AuthPresentation {

    private static final String REGISTER_VIEW = "register";
    private static final String PASSWORD_RECOVERY_RESET_VIEW = "password-recovery-reset";

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
        return mav;
    }

    public ModelAndView registerForm(final RegisterForm form) {
        return new ModelAndView(REGISTER_VIEW);
    }

    public ModelAndView registerSubmit(final RegisterForm form, final BindingResult errors) {
        userService.register(
                trim(form.getGivenName()),
                trim(form.getLastName()),
                trim(form.getEmail()),
                form.getPaymentAlias(),
                languageFromInput(form.getPreferredLanguage()),
                form.getPassword());
        return new ModelAndView("redirect:/login?verificationSent=true");
    }

    public ModelAndView passwordRecoveryRequestForm(final PasswordRecoveryRequestForm form) {
        final ModelAndView mav = new ModelAndView("password-recovery-request");
        if (form.getSent() != null) {
            mav.addObject("recoverySent", true);
        }
        return mav;
    }

    public ModelAndView passwordRecoveryRequestSubmit(final PasswordRecoveryRequestForm form) {
        final String email = form.getEmail() == null ? null : form.getEmail().trim();
        userService.requestPasswordRecovery(email);
        return new ModelAndView("redirect:/password-recovery?sent=true");
    }

    public ModelAndView passwordRecoveryResetForm(final String token) {
        final Optional<Users> user = userService.findByPasswordRecoveryToken(token);
        return user.isEmpty() || user.get().getMailTokenEmittedAt() != null
                ? new ModelAndView("redirect:/password-recovery/" + token + "?invalid=true")
                : new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                        .addObject("token", token)
                        .addObject("tokenValid", true);
    }

    public ModelAndView passwordRecoveryResetSubmit(final String token, final String rawPassword) {
        final boolean tokenValid =
                userService.findByPasswordRecoveryToken(token).isPresent();
        if (!tokenValid) {
            return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                    .addObject("token", token)
                    .addObject("tokenValid", false);
        }

        if (!userService.resetPassword(token, rawPassword)) {
            return new ModelAndView("redirect:/password-recovery/" + token + "?invalid=true");
        }

        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    public ModelAndView verifyEmail(
            final String token, final HttpServletRequest request, final HttpServletResponse response) {
        final Optional<Users> verifiedUser = userService.verifyEmail(token);
        if (verifiedUser.isPresent()) {
            postRegistrationAuthenticator.authenticateVerifiedUser(
                    verifiedUser.get().getEmail(), request, response);
            return new ModelAndView("redirect:/");
        }
        return new ModelAndView("redirect:/login?verificationInvalid=true");
    }

    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }

    // TODO check this, should be bc of UserDetails SpringSecurity probs
    private static String languageFromInput(final String preferredLanguage) {
        if (preferredLanguage == null) {
            return "ES";
        }
        return switch (preferredLanguage.trim().toUpperCase()) {
            case "EN" -> "EN";
            default -> "ES";
        };
    }

    private static String trim(final String value) {
        return value == null ? null : value.trim();
    }
}
