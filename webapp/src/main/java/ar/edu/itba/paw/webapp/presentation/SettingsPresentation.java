package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.webapp.form.SettingsForm;
import ar.edu.itba.paw.webapp.form.SettingsViewForm;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

public final class SettingsPresentation {

    private static final String MESSAGE_PREFIX = "settings";

    private SettingsPresentation() {}

    public static ModelAndView passwordRecoverySentRedirect() {
        return new ModelAndView("redirect:/settings?passwordRecovery=sent");
    }

    public static ModelAndView settingsView(
            final Users user, final SettingsForm form, final boolean edit, final PageModel<Users> subscriptions) {
        if (form.getEmail() == null) {
            form.setGivenName(user.getFirstName());
            form.setLastName(user.getLastName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setPaymentAlias(user.getAlias());
            form.setPreferredLanguage(
                    PreferredLanguageModel.fromPersistence(user.getLanguage()).getPersistenceCode());
        }
        return buildSettingsView(user, edit, subscriptions);
    }

    public static ModelAndView settingsViewErrors(
            final Users user,
            final SettingsForm form,
            final SettingsViewForm settingsView,
            final BindingResult errors,
            final MessageSource messageSource) {
        if (form.getEmail() == null) {
            form.setGivenName(user.getFirstName());
            form.setLastName(user.getLastName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setPaymentAlias(user.getAlias());
            form.setPreferredLanguage(
                    PreferredLanguageModel.fromPersistence(user.getLanguage()).getPersistenceCode());
        }
        final boolean edit = Boolean.TRUE.equals(settingsView.getEdit());
        final ModelAndView mav = buildSettingsView(user, edit, new PageModel<>(List.of(), 1, 6, 0L));
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", ToastPresentation.validationToasts(errors, MESSAGE_PREFIX, messageSource));
        mav.addObject("settingsView", settingsView);
        mav.addObject("hasValidationErrors", true);
        return mav;
    }

    public static ModelAndView settingsEditView(final Users user, final PageModel<Users> subscriptions) {
        return buildSettingsView(user, true, subscriptions);
    }

    public static ModelAndView settingsUpdatedRedirect() {
        return new ModelAndView("redirect:/settings?settingsAction=updated");
    }

    public static ModelAndView settingsVerificationSentRedirect() {
        return new ModelAndView("redirect:/settings?settingsAction=verificationSent");
    }

    private static ModelAndView buildSettingsView(
            final Users user, final boolean settingsEdit, final PageModel<Users> subscriptions) {
        final ModelAndView mav = new ModelAndView("settings");
        mav.addObject("user", user);
        mav.addObject("subscriptionsPage", subscriptions);
        mav.addObject("settingsEdit", settingsEdit);
        mav.addObject("settingsView", settingsView(subscriptions, settingsEdit));
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    private static SettingsViewForm settingsView(final PageModel<Users> subscriptions, final boolean settingsEdit) {
        final SettingsViewForm view = new SettingsViewForm();
        view.setPage(subscriptions.getPage());
        view.setPageSize(subscriptions.getPageSize());
        view.setEdit(settingsEdit);
        return view;
    }
}
