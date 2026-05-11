package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class AuthPresentation {

    private static final String REGISTER_VIEW = "nuevo/register";
    private static final String PASSWORD_RECOVERY_RESET_VIEW = "nuevo/password-recovery-reset";

    private final UserService userService;
    private final AuthModelMapper authModelMapper;

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
        final UserModel user = authModelMapper.fromRegisterForm(form);
        final String rawPassword = form.getPassword();
        if (userService.register(user, rawPassword) != UserService.RegistrationResult.SUCCESS) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView(REGISTER_VIEW);
        }

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
        final UserModel user = authModelMapper.fromPasswordRecoveryRequestForm(form);
        userService.requestPasswordRecovery(user);
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
        final UserModel user = authModelMapper.fromPasswordResetForm(form);
        final String rawPassword = form.getPassword();
        final boolean tokenValid = userService
                .findByPasswordRecoveryToken(user.getPasswordRecoveryToken())
                .isPresent();
        if (!tokenValid) {
            return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                    .addObject("token", user.getPasswordRecoveryToken())
                    .addObject("tokenValid", false);
        }

        if (userService.resetPassword(user, rawPassword) != UserService.PasswordRecoveryResult.SUCCESS) {
            return new ModelAndView("redirect:/password-recovery/" + user.getPasswordRecoveryToken() + "?invalid=true");
        }

        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    public ModelAndView verifyEmail(final String token) {
        if (userService.verifyEmail(token).isPresent()) {
            return new ModelAndView("redirect:/login?verified=true");
        }
        return new ModelAndView("redirect:/login?verificationInvalid=true");
    }

    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }
}
