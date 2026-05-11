package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.form.nuevo.ProfileForm;
import java.time.OffsetDateTime;
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
    private final ProfileModelMapper profileModelMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView profilePasswordRecoveryRequest() {
        final UserModel user = authenticatedUserResolver.currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        userService.requestPasswordRecovery(user);
        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    public ModelAndView profile(final boolean edit, final ProfileForm form) {
        final UserModel user = authenticatedUserResolver.currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        profileModelMapper.populateProfileForm(form, user);
        return buildProfileView(user, edit);
    }

    public ModelAndView profileSubmit(final ProfileForm form, final BindingResult errors) {
        final UserModel currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true);
        }

        final UserModel profileUpdate = profileModelMapper.fromProfileForm(form, currentUser.getId());
        final UserModel updatedUser = userService.updateProfile(profileUpdate).orElse(null);
        if (updatedUser == null) {
            errors.rejectValue("email", "profile.validation.email.duplicate");
            return buildProfileView(currentUser, true);
        }

        refreshAuthenticatedPrincipal(updatedUser);
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    private ModelAndView buildProfileView(final UserModel user, final boolean profileEdit) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }

    private static void refreshAuthenticatedPrincipal(final UserModel user) {
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
