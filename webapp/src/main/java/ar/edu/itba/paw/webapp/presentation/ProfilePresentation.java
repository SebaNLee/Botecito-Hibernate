package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private final UserService userService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView profilePasswordRecoveryRequest() {
        final UsersOrm user = authenticatedUserResolver.currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        userService.requestPasswordRecovery(user.getEmail());
        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    public ModelAndView profile(final boolean edit, final ProfileForm form) {
        final UsersOrm user = authenticatedUserResolver.currentAuthenticatedUser();
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
        return buildProfileView(user, edit);
    }

    public ModelAndView profileSubmit(final ProfileForm form, final BindingResult errors) {
        final UsersOrm currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true);
        }

        final UsersOrm updatedUser = userService
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
            return buildProfileView(currentUser, true);
        }

        refreshAuthenticatedPrincipal(updatedUser);
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return new ModelAndView("redirect:/profile?profileAction=verificationSent");
        }
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    private ModelAndView buildProfileView(final UsersOrm user, final boolean profileEdit) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }

    private static void refreshAuthenticatedPrincipal(final UsersOrm user) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || user == null || user.getEmail() == null) {
            return;
        }

        final UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                user.getEmail(), authentication.getCredentials(), authentication.getAuthorities());
        refreshed.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }

    private static String formatMemberSince(final LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // TODO check this, should be bc of UserDetails SpringSecurity probs
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
