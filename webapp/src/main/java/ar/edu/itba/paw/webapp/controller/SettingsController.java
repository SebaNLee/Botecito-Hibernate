package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.auth.SecurityContextRefresher;
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

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SecurityContextRefresher securityContextRefresher;
    private final SettingsPresentation settingsPresentation;

    @RequestMapping(value = "/settings/password-recovery", method = RequestMethod.POST)
    public ModelAndView settingsPasswordRecoveryRequest(@AuthenticationPrincipal final BotecitoUserDetails user) {
        userService.requestPasswordRecovery(user.getEmail());
        return settingsPresentation.passwordRecoverySentRedirect();
    }

    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView settings(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @RequestParam(value = "edit", defaultValue = "false") final boolean edit,
            @RequestParam(value = "subscriptionsPage", defaultValue = "1") final int subscriptionsPage,
            @RequestParam(value = "subscriptionsPageSize", defaultValue = "6") final int subscriptionsPageSize,
            @ModelAttribute("settingsForm") final SettingsForm form) {
        final Users currentUser = userService.findById(user.getId()).orElseThrow();
        final PageModel<Users> subscriptions =
                listSubscriptions(user.getId(), subscriptionsPage, subscriptionsPageSize);
        return settingsPresentation.settingsView(currentUser, form, edit, subscriptions);
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

        final Users currentUser = userService.findById(user.getId()).orElseThrow();
        final PageModel<Users> subscriptions =
                listSubscriptions(user.getId(), subscriptionsPage, subscriptionsPageSize);

        if (errors.hasErrors()) {
            return settingsPresentation.settingsEditView(currentUser, subscriptions);
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
            return settingsPresentation.settingsEditView(currentUser, subscriptions);
        }

        if (!updatedUser.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            securityContextRefresher.refreshPrincipal(updatedUser.getEmail());
        }
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return settingsPresentation.settingsVerificationSentRedirect();
        }
        return settingsPresentation.settingsUpdatedRedirect();
    }

    private PageModel<Users> listSubscriptions(
            final int userId, final int subscriptionsPage, final int subscriptionsPageSize) {
        final int safeSubscriptionsPage = Math.max(1, subscriptionsPage);
        final int safeSubscriptionsPageSize = Math.max(1, subscriptionsPageSize);
        return subscriptionService.listSubscriptions(userId, safeSubscriptionsPage, safeSubscriptionsPageSize);
    }
}
