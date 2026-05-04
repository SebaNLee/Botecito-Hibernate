package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public final class ProfileMvcSupport {

    private final UserService userService;

    public ModelAndView profilePasswordRecoveryRequest() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ModelAndView("redirect:/login");
        }

        userService
                .findByEmail(authentication.getName())
                .ifPresent(user -> userService.requestPasswordRecovery(user.getEmail()));

        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    public ModelAndView profile(final boolean edit, final ProfileForm form) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        populateProfileForm(form, user);
        return buildProfileView(user, edit);
    }

    public ModelAndView profileSubmit(final ProfileForm form, final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true);
        }

        final User updatedUser = userService
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
            errors.rejectValue("email", "profile.validation.email.duplicate");
            return buildProfileView(currentUser, true);
        }

        refreshAuthenticatedPrincipal(updatedUser);
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    private ModelAndView buildProfileView(final User user, final boolean profileEdit) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }

    private static void populateProfileForm(final ProfileForm form, final User user) {
        if (form == null || user == null || form.getEmail() != null) {
            return;
        }

        form.setGivenName(user.getGivenName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setPaymentAlias(user.getPaymentAlias());
        form.setPreferredLanguage(
                user.getPreferredLanguage() == null
                        ? null
                        : user.getPreferredLanguage().getPersistenceCode());
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private static void refreshAuthenticatedPrincipal(final User user) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || user == null || user.getEmail() == null) {
            return;
        }

        final UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                user.getEmail(), authentication.getCredentials(), authentication.getAuthorities());
        refreshed.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }

    private static String formatMemberSince(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
