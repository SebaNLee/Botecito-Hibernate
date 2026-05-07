package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

// @Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PostRegistrationAuthenticator postRegistrationAuthenticator;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "logout", required = false) final String logout,
            @RequestParam(value = "registered", required = false) final String registered,
            @RequestParam(value = "legacyToken", required = false) final String legacyToken,
            @RequestParam(value = "passwordRecovered", required = false) final String passwordRecovered) {

        final ModelAndView mav = new ModelAndView("login");
        if (error != null) {
            mav.addObject("loginError", true);
        }
        if (logout != null) {
            mav.addObject("logoutSuccess", true);
        }
        if (registered != null) {
            mav.addObject("registeredSuccess", true);
        }
        if (legacyToken != null) {
            mav.addObject("legacyTokenError", true);
        }
        if (passwordRecovered != null) {
            mav.addObject("passwordRecoveredSuccess", true);
        }
        return mav;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView registerForm(@ModelAttribute("registerForm") final RegisterForm form) {
        return new ModelAndView("register");
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerSubmit(
            @Valid @ModelAttribute("registerForm") final RegisterForm form,
            final BindingResult errors,
            final HttpServletRequest request) {

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "register.validation.password.mismatch");
        }

        if (errors.hasErrors()) {
            return new ModelAndView("register");
        }

        if (userService.register(
                        form.getGivenName().trim(),
                        form.getLastName().trim(),
                        form.getEmail().trim(),
                        form.getPassword(),
                        form.getPaymentAlias(),
                        request.getLocale().getLanguage())
                != UserService.RegistrationResult.SUCCESS) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView("register");
        }

        if (!postRegistrationAuthenticator.authenticate(form.getEmail().trim(), form.getPassword(), request)) {
            return new ModelAndView("redirect:/login?registered=true");
        }
        return new ModelAndView("redirect:/");
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryRequestForm(
            @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form,
            @RequestParam(value = "sent", required = false) final String sent) {
        final ModelAndView mav = new ModelAndView("password-recovery-request");
        if (sent != null) {
            mav.addObject("recoverySent", true);
        }
        return mav;
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryRequestSubmit(
            @Valid @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("password-recovery-request");
        }

        userService.requestPasswordRecovery(form.getEmail().trim());

        return new ModelAndView("redirect:/password-recovery?sent=true");
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryResetForm(
            @PathVariable("token") final String token,
            @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            @RequestParam(value = "invalid", required = false) final String invalid) {
        final ModelAndView mav = new ModelAndView("password-recovery-reset");
        mav.addObject("token", token);
        mav.addObject(
                "tokenValid", userService.findByPasswordRecoveryToken(token).isPresent());
        if (invalid != null) {
            mav.addObject("tokenInvalidError", true);
        }
        return mav;
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryResetSubmit(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            final BindingResult errors) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "passwordRecovery.reset.validation.password.mismatch");
        }

        final boolean tokenValid =
                userService.findByPasswordRecoveryToken(token).isPresent();
        if (!tokenValid) {
            return new ModelAndView("password-recovery-reset")
                    .addObject("token", token)
                    .addObject("tokenValid", false);
        }

        if (errors.hasErrors()) {
            return new ModelAndView("password-recovery-reset")
                    .addObject("token", token)
                    .addObject("tokenValid", true);
        }

        if (userService.resetPassword(token, form.getPassword()) != UserService.PasswordRecoveryResult.SUCCESS) {
            return new ModelAndView("redirect:/password-recovery/" + token + "?invalid=true");
        }

        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    @RequestMapping("/403")
    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }
}
