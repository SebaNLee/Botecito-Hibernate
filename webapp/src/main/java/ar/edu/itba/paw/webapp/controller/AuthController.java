package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.LoginForm;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import ar.edu.itba.paw.webapp.presentation.AuthPresentation;
import ar.edu.itba.paw.webapp.util.PostLoginRedirectSupport;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final String PASSWORD_RECOVERY_RESET_VIEW = "password-recovery-reset";

    private final UserService userService;
    private final PostRegistrationAuthenticator postRegistrationAuthenticator;
    private final AuthPresentation authPresentation;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(@ModelAttribute("loginForm") final LoginForm form, final HttpServletRequest request) {
        final String next = form.getNext();
        if (next != null) {
            if (PostLoginRedirectSupport.isSafeInternalRedirect(next)) {
                request.getSession().setAttribute(PostLoginRedirectSupport.SESSION_ATTR, next.trim());
            } else {
                request.getSession().removeAttribute(PostLoginRedirectSupport.SESSION_ATTR);
            }
        }
        return authPresentation.login(form);
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView registerForm(@ModelAttribute("registerForm") final RegisterForm form) {
        return authPresentation.registerForm(form);
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerSubmit(
            @Valid @ModelAttribute("registerForm") final RegisterForm form,
            final BindingResult errors,
            final HttpServletRequest request) {
        if (errors.hasErrors()) {
            return new ModelAndView("register");
        }
        form.setPreferredLanguage(request.getLocale().getLanguage());
        userService.register(
                form.getGivenName(),
                form.getLastName(),
                form.getEmail(),
                form.getPaymentAlias(),
                languageFromInput(form.getPreferredLanguage()),
                form.getPassword());
        return authPresentation.registerSuccessRedirect();
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryRequestForm(
            @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form) {
        return authPresentation.passwordRecoveryRequestForm(form);
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryRequestSubmit(
            @Valid @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("password-recovery-request");
        }
        userService.requestPasswordRecovery(form.getEmail());
        return authPresentation.passwordRecoveryRequestSuccessRedirect();
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryResetForm(@PathVariable("token") final String token) {
        final Optional<Users> user = userService.findByPasswordRecoveryToken(token);
        if (user.isEmpty() || user.get().getMailTokenEmittedAt() != null) {
            return authPresentation.passwordRecoveryResetInvalidTokenRedirect(token);
        }
        return authPresentation.passwordRecoveryResetForm(token);
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryResetSubmit(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView(PASSWORD_RECOVERY_RESET_VIEW)
                    .addObject("token", token)
                    .addObject("tokenValid", true)
                    .addObject("org.springframework.validation.BindingResult.passwordResetForm", errors);
        }
        final boolean tokenValid =
                userService.findByPasswordRecoveryToken(token).isPresent();
        if (!tokenValid) {
            return authPresentation.passwordRecoveryResetInvalidTokenView(token);
        }
        if (!userService.resetPassword(token, form.getPassword())) {
            return authPresentation.passwordRecoveryResetInvalidTokenRedirect(token);
        }
        return authPresentation.passwordRecoveryResetSuccessRedirect();
    }

    @RequestMapping(value = "/verify-email/{token}", method = RequestMethod.GET)
    public ModelAndView verifyEmail(
            @PathVariable("token") final String token,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final Optional<Users> verifiedUser = userService.verifyEmail(token);
        if (verifiedUser.isPresent()) {
            postRegistrationAuthenticator.authenticateVerifiedUser(
                    verifiedUser.get().getEmail(), request, response);
            return authPresentation.verifyEmailSuccessRedirect();
        }
        return authPresentation.verifyEmailInvalidRedirect();
    }

    private static String languageFromInput(final String preferredLanguage) {
        if (preferredLanguage == null) {
            return "ES";
        }
        return switch (preferredLanguage.trim().toUpperCase()) {
            case "EN" -> "EN";
            default -> "ES";
        };
    }
}
