package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfilePresentation profilePresentation;

    @RequestMapping(value = "/profile/password-recovery", method = RequestMethod.POST)
    public ModelAndView profilePasswordRecoveryRequest(@AuthenticationPrincipal final BotecitoUserDetails user) {
        return profilePresentation.profilePasswordRecoveryRequest(user);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @RequestParam(value = "edit", defaultValue = "false") final boolean edit,
            @ModelAttribute("profileForm") final ProfileForm form) {
        return profilePresentation.profile(user, edit, form);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ModelAndView profileSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("profileForm") final ProfileForm form,
            final BindingResult errors,
            final HttpServletRequest request) {
        request.getSession().removeAttribute("userLocale");
        if (errors.hasErrors()) {
            return profilePresentation.profileSubmit(user, form, errors);
        }
        return profilePresentation.profileSubmit(user, form, errors);
    }
}
