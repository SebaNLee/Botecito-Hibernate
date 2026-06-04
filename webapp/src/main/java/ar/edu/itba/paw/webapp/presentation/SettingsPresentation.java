package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.webapp.form.SettingsForm;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class SettingsPresentation {

    public ModelAndView passwordRecoverySentRedirect() {
        return new ModelAndView("redirect:/settings?passwordRecovery=sent");
    }

    public ModelAndView settingsView(
            final Users user, final SettingsForm form, final boolean edit, final PageModel<Users> subscriptions) {
        if (form.getEmail() == null) {
            form.setGivenName(user.getFirstName());
            form.setLastName(user.getLastName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setPaymentAlias(user.getAlias());
            form.setPreferredLanguage(user.getLanguage());
        }
        return buildSettingsView(user, edit, subscriptions);
    }

    public ModelAndView settingsEditView(final Users user, final PageModel<Users> subscriptions) {
        return buildSettingsView(user, true, subscriptions);
    }

    public ModelAndView settingsUpdatedRedirect() {
        return new ModelAndView("redirect:/settings?settingsAction=updated");
    }

    public ModelAndView settingsVerificationSentRedirect() {
        return new ModelAndView("redirect:/settings?settingsAction=verificationSent");
    }

    private ModelAndView buildSettingsView(
            final Users user, final boolean settingsEdit, final PageModel<Users> subscriptions) {
        final ModelAndView mav = new ModelAndView("settings");
        mav.addObject("user", user);
        mav.addObject("subscriptionsPage", subscriptions);
        mav.addObject("subscriptions", subscriptions.getContent());
        mav.addObject("memberSinceDisplay", user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        mav.addObject("settingsEdit", settingsEdit);
        return mav;
    }
}
