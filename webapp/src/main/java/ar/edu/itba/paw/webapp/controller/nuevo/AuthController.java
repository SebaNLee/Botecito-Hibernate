package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import ar.edu.itba.paw.webapp.presentation.AuthPresentation;
import ar.edu.itba.paw.webapp.util.PostLoginRedirectSupport;
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

    private static final String PASSWORD_RECOVERY_RESET_VIEW = "nuevo/password-reset";

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
            return new ModelAndView("nuevo/register");
        }
        form.setPreferredLanguage(request.getLocale().getLanguage());
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
    public ModelAndView passwordRecoveryResetForm(@PathVariable("token") final String token) {
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
        return authPresentation.passwordRecoveryResetSubmit(token, form.getPassword());
    }

    @RequestMapping(value = "/verify-email/{token}", method = RequestMethod.GET)
    public ModelAndView verifyEmail(
            @PathVariable("token") final String token,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return authPresentation.verifyEmail(token, request, response);
    }

}
