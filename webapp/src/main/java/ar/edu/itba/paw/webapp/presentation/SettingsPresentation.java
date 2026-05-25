package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.auth.SecurityContextRefresher;
import ar.edu.itba.paw.webapp.form.SettingsForm;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class SettingsPresentation {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SecurityContextRefresher securityContextRefresher;

    public ModelAndView settingsPasswordRecoveryRequest(final BotecitoUserDetails principal) {
        userService.requestPasswordRecovery(principal.getEmail());
        return new ModelAndView("redirect:/settings?passwordRecovery=sent");
    }

    public ModelAndView settings(
            final BotecitoUserDetails principal,
            final boolean edit,
            final int subscriptionsPage,
            final int subscriptionsPageSize,
            final SettingsForm form) {
        final Users user = userService.findById(principal.getId()).orElseThrow();

        if (form.getEmail() == null) {
            form.setGivenName(user.getFirstName());
            form.setLastName(user.getLastName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setPaymentAlias(user.getAlias());
            form.setPreferredLanguage(user.getLanguage());
        }
        return buildSettingsView(user, edit, subscriptionsPage, subscriptionsPageSize);
    }

    public ModelAndView settingsSubmit(
            final BotecitoUserDetails principal,
            final SettingsForm form,
            final BindingResult errors,
            final int subscriptionsPage,
            final int subscriptionsPageSize) {
        final Users currentUser = userService.findById(principal.getId()).orElseThrow();

        if (errors.hasErrors()) {
            return buildSettingsView(currentUser, true, subscriptionsPage, subscriptionsPageSize);
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
            return buildSettingsView(currentUser, true, subscriptionsPage, subscriptionsPageSize);
        }

        if (!updatedUser.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            securityContextRefresher.refreshPrincipal(updatedUser.getEmail());
        }
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return new ModelAndView("redirect:/settings?settingsAction=verificationSent");
        }
        return new ModelAndView("redirect:/settings?settingsAction=updated");
    }

    private ModelAndView buildSettingsView(
            final Users user, final boolean settingsEdit, final int subscriptionsPage, final int subscriptionsPageSize) {
        final ModelAndView mav = new ModelAndView("settings");
        final int safeSubscriptionsPage = Math.max(1, subscriptionsPage);
        final int safeSubscriptionsPageSize = Math.max(1, subscriptionsPageSize);
        final PageModel<Users> subscriptions =
                subscriptionService.listSubscriptions(user.getId(), safeSubscriptionsPage, safeSubscriptionsPageSize);
        mav.addObject("user", user);
        mav.addObject("subscriptionsPage", subscriptions);
        mav.addObject("subscriptions", subscriptions.getContent());
        mav.addObject("memberSinceDisplay", user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        mav.addObject("settingsEdit", settingsEdit);
        return mav;
    }
}
