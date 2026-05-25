package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView profilePasswordRecoveryRequest(final BotecitoUserDetails principal) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users user = authenticatedUserResolver.loadUser(principal);
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        userService.requestPasswordRecovery(user.getEmail());
        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    public ModelAndView profile(
            final BotecitoUserDetails principal,
            final boolean edit,
            final int subscriptionsPage,
            final int subscriptionsPageSize,
            final ProfileForm form) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users user = authenticatedUserResolver.loadUser(principal);
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        if (form.getEmail() == null) {
            form.setGivenName(user.getFirstName());
            form.setLastName(user.getLastName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setPaymentAlias(user.getAlias());
            form.setPreferredLanguage(user.getLanguage());
        }
        return buildProfileView(user, edit, subscriptionsPage, subscriptionsPageSize);
    }

    public ModelAndView profileSubmit(
            final BotecitoUserDetails principal,
            final ProfileForm form,
            final BindingResult errors,
            final int subscriptionsPage,
            final int subscriptionsPageSize) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users currentUser = authenticatedUserResolver.loadUser(principal);
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true, subscriptionsPage, subscriptionsPageSize);
        }

        final Users updatedUser = userService
                .updateProfile(
                        currentUser.getId(),
                        trim(form.getGivenName()),
                        trim(form.getLastName()),
                        trim(form.getEmail()),
                        form.getPhone(),
                        form.getPaymentAlias(),
                        languageFromInput(form.getPreferredLanguage()))
                .orElse(null);
        if (updatedUser == null) {
            errors.rejectValue("email", "profile.validation.email.duplicate");
            return buildProfileView(currentUser, true, subscriptionsPage, subscriptionsPageSize);
        }

        authenticatedUserResolver.refreshPrincipal(updatedUser.getEmail());
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return new ModelAndView("redirect:/profile?profileAction=verificationSent");
        }
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    private ModelAndView buildProfileView(
            final Users user, final boolean profileEdit, final int subscriptionsPage, final int subscriptionsPageSize) {
        final ModelAndView mav = new ModelAndView("profile");
        final int safeSubscriptionsPage = Math.max(1, subscriptionsPage);
        final int safeSubscriptionsPageSize = Math.max(1, subscriptionsPageSize);
        final PageModel<Users> subscriptions =
                subscriptionService.listSubscriptions(user.getId(), safeSubscriptionsPage, safeSubscriptionsPageSize);
        mav.addObject("user", user);
        mav.addObject("subscriptionsPage", subscriptions);
        mav.addObject("subscriptions", subscriptions.getContent());
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }

    private static String formatMemberSince(final LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String languageFromInput(final String preferredLanguage) {
        if (preferredLanguage == null) {
            return "ES";
        }
        return switch (preferredLanguage.trim().toUpperCase()) {
            case "EN" -> "EN";
            default -> "ES";
        };
    }

    private static String trim(final String value) {
        return value == null ? null : value.trim();
    }
}
