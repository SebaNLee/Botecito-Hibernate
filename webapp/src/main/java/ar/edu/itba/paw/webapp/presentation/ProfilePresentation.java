package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.auth.SecurityContextRefresher;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private final UserService userService;
    private final SecurityContextRefresher securityContextRefresher;

    public ModelAndView profilePasswordRecoveryRequest(final BotecitoUserDetails principal) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users user = userService.findById(principal.getId()).orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        userService.requestPasswordRecovery(user.getEmail());
        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    public ModelAndView profile(final BotecitoUserDetails principal, final boolean edit, final ProfileForm form) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users user = userService.findById(principal.getId()).orElse(null);
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

    public ModelAndView profileSubmit(
            final BotecitoUserDetails principal, final ProfileForm form, final BindingResult errors) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        final Users currentUser = userService.findById(principal.getId()).orElse(null);
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true);
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
            errors.rejectValue("email", "profile.validation.email.duplicate");
            return buildProfileView(currentUser, true);
        }

        if (!updatedUser.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            securityContextRefresher.refreshPrincipal(updatedUser.getEmail());
        }
        if (updatedUser.getVerified() == null || !updatedUser.getVerified()) {
            return new ModelAndView("redirect:/profile?profileAction=verificationSent");
        }
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    private ModelAndView buildProfileView(final Users user, final boolean profileEdit) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }
}
