package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.auth.SecurityContextRefresher;
import ar.edu.itba.paw.webapp.form.SettingsForm;
import ar.edu.itba.paw.webapp.form.SettingsViewForm;
import ar.edu.itba.paw.webapp.presentation.SettingsPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SecurityContextRefresher securityContextRefresher;
    private final MessageSource messageSource;

    @ModelAttribute("settingsView")
    public SettingsViewForm defaultSettingsView() {
        final SettingsViewForm form = new SettingsViewForm();
        form.setPage(1);
        form.setPageSize(6);
        form.setEdit(false);
        return form;
    }

    @RequestMapping(value = "/settings/password-recovery", method = RequestMethod.POST)
    public ModelAndView settingsPasswordRecoveryRequest(@AuthenticationPrincipal final BotecitoUserDetails user) {
        userService.requestPasswordRecovery(user.getEmail());
        return SettingsPresentation.passwordRecoverySentRedirect();
    }

    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView settings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("settingsView") final SettingsViewForm settingsView,
            final BindingResult viewErrors,
            @ModelAttribute("settingsForm") final SettingsForm form) {
        final Users currentUser = userService.findById(user.getId()).orElseThrow();
        if (viewErrors.hasErrors()) {
            return SettingsPresentation.settingsViewErrors(currentUser, form, settingsView, viewErrors, messageSource);
        }
        final var subscriptions =
                subscriptionService.listSubscriptions(user.getId(), settingsView.getPage(), settingsView.getPageSize());
        final boolean edit = Boolean.TRUE.equals(settingsView.getEdit());
        return SettingsPresentation.settingsView(currentUser, form, edit, subscriptions);
    }

    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    public ModelAndView settingsSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("settingsForm") final SettingsForm form,
            final BindingResult errors,
            @Valid @ModelAttribute("settingsView") final SettingsViewForm settingsView,
            final BindingResult viewErrors,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        request.getSession().removeAttribute("userLocale");

        final Users currentUser = userService.findById(user.getId()).orElseThrow();
        if (viewErrors.hasErrors()) {
            return SettingsPresentation.settingsViewErrors(currentUser, form, settingsView, viewErrors, messageSource);
        }
        final var subscriptions =
                subscriptionService.listSubscriptions(user.getId(), settingsView.getPage(), settingsView.getPageSize());

        if (errors.hasErrors()) {
            return SettingsPresentation.settingsEditView(currentUser, subscriptions);
        }

        final Users updatedUser = userService
                .updateProfile(
                        currentUser.getId(),
                        form.getGivenName(),
                        form.getLastName(),
                        form.getEmail(),
                        form.getPhone(),
                        form.getPaymentAlias(),
                        form.getPreferredLanguage())
                .orElse(null);
        if (updatedUser == null) {
            errors.rejectValue("email", "settings.validation.email.duplicate");
            return SettingsPresentation.settingsEditView(currentUser, subscriptions);
        }

        if (!updatedUser.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            securityContextRefresher.refreshPrincipal(updatedUser.getEmail(), request, response);
        }
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return SettingsPresentation.settingsVerificationSentRedirect();
        }
        return SettingsPresentation.settingsUpdatedRedirect();
    }
}
