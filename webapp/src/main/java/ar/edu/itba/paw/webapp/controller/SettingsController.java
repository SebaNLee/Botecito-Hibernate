package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.SettingsForm;
import ar.edu.itba.paw.webapp.presentation.SettingsPresentation;
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
public class SettingsController {

    private final SettingsPresentation settingsPresentation;

    @RequestMapping(value = "/settings/password-recovery", method = RequestMethod.POST)
    public ModelAndView settingsPasswordRecoveryRequest(@AuthenticationPrincipal final BotecitoUserDetails user) {
        return settingsPresentation.settingsPasswordRecoveryRequest(user);
    }

    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView settings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @RequestParam(value = "edit", defaultValue = "false") final boolean edit,
            @RequestParam(value = "subscriptionsPage", defaultValue = "1") final int subscriptionsPage,
            @RequestParam(value = "subscriptionsPageSize", defaultValue = "6") final int subscriptionsPageSize,
            @ModelAttribute("settingsForm") final SettingsForm form) {
        return settingsPresentation.settings(user, edit, subscriptionsPage, subscriptionsPageSize, form);
    }

    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    public ModelAndView settingsSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("settingsForm") final SettingsForm form,
            final BindingResult errors,
            @RequestParam(value = "subscriptionsPage", defaultValue = "1") final int subscriptionsPage,
            @RequestParam(value = "subscriptionsPageSize", defaultValue = "6") final int subscriptionsPageSize,
            final HttpServletRequest request) {
        request.getSession().removeAttribute("userLocale");
        return settingsPresentation.settingsSubmit(user, form, errors, subscriptionsPage, subscriptionsPageSize);
    }
}
