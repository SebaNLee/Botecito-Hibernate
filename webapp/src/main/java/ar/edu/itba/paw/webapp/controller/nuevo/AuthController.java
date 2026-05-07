package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import ar.edu.itba.paw.webapp.presentation.nuevo.AuthModelMapper;
import ar.edu.itba.paw.webapp.presentation.nuevo.AuthPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    private final AuthPresentation authPresentation;

    public AuthController(
            final UserService userService, final PostRegistrationAuthenticator postRegistrationAuthenticator) {
        this.authPresentation = new AuthPresentation(userService, postRegistrationAuthenticator, new AuthModelMapper());
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(@ModelAttribute("loginForm") final LoginForm form) {
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
            return new ModelAndView("nuevo/register");
        }
        form.setPreferredLanguage(request.getLocale().getLanguage());
        form.setRequest(request);
        return authPresentation.registerSubmit(form, errors);
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
        return authPresentation.passwordRecoveryRequestSubmit(form);
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryResetForm(
            @PathVariable("token") final String token,
            @ModelAttribute("passwordResetForm") final PasswordResetForm form) {
        form.setToken(token);
        return authPresentation.passwordRecoveryResetForm(form);
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryResetSubmit(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            form.setToken(token);
            return authPresentation.passwordRecoveryResetWithErrors(form);
        }
        form.setToken(token);
        return authPresentation.passwordRecoveryResetSubmit(form);
    }

    @RequestMapping("/403")
    public ModelAndView forbidden() {
        return authPresentation.forbidden();
    }
}
