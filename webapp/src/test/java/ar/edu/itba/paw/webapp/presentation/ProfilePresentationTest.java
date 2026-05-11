package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.form.nuevo.ProfileForm;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class ProfilePresentationTest {

    @Mock
    private UserService userService;

    private ProfilePresentation profilePresentation;

    @BeforeEach
    public void setUp() {
        profilePresentation = new ProfilePresentation(userService, new ProfileModelMapper());
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testProfileRedirectsToLoginWhenUnauthenticated() {
        final ModelAndView mav = profilePresentation.profile(false, new ProfileForm());

        Assertions.assertEquals("redirect:/login", mav.getViewName());
    }

    @Test
    public void testProfilePopulatesFormAndViewModel() {
        authenticate("ada@example.com");
        final UserModel user = user(7, "Ada", "Lovelace", "ada@example.com");
        user.setCreatedAt(OffsetDateTime.parse("2026-01-02T12:00:00Z"));
        user.setPhone("123");
        user.setPaymentAlias("alias");
        user.setPreferredLanguage(PreferredLanguageModel.EN);
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        final ProfileForm form = new ProfileForm();

        final ModelAndView mav = profilePresentation.profile(true, form);

        Assertions.assertEquals("profile", mav.getViewName());
        Assertions.assertSame(user, mav.getModel().get("user"));
        Assertions.assertEquals("02/01/2026", mav.getModel().get("memberSinceDisplay"));
        Assertions.assertEquals(true, mav.getModel().get("profileEdit"));
        Assertions.assertEquals("Ada", form.getGivenName());
        Assertions.assertEquals("Lovelace", form.getLastName());
        Assertions.assertEquals("ada@example.com", form.getEmail());
        Assertions.assertEquals("123", form.getPhone());
        Assertions.assertEquals("alias", form.getPaymentAlias());
        Assertions.assertEquals("en", form.getPreferredLanguage());
    }

    @Test
    public void testProfileSubmitReturnsEditViewWhenBindingHasErrors() {
        authenticate("ada@example.com");
        final UserModel user = user(7, "Ada", "Lovelace", "ada@example.com");
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        final ProfileForm form = validProfileForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "profileForm");
        errors.rejectValue("email", "profile.validation.email.invalid");

        final ModelAndView mav = profilePresentation.profileSubmit(form, errors);

        Assertions.assertEquals("profile", mav.getViewName());
        Assertions.assertEquals(true, mav.getModel().get("profileEdit"));
        Mockito.verify(userService, Mockito.never()).updateProfile(Mockito.any(UserModel.class));
    }

    @Test
    public void testProfileSubmitRejectsDuplicateEmail() {
        authenticate("ada@example.com");
        final UserModel currentUser = user(7, "Ada", "Lovelace", "ada@example.com");
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(currentUser));
        Mockito.when(userService.updateProfile(Mockito.any(UserModel.class))).thenReturn(Optional.empty());
        final ProfileForm form = validProfileForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "profileForm");

        final ModelAndView mav = profilePresentation.profileSubmit(form, errors);

        Assertions.assertEquals("profile", mav.getViewName());
        Assertions.assertTrue(errors.hasFieldErrors("email"));
    }

    @Test
    public void testProfileSubmitUpdatesAndRefreshesAuthenticatedEmail() {
        authenticate("ada@example.com");
        final UserModel currentUser = user(7, "Ada", "Lovelace", "ada@example.com");
        final UserModel updatedUser = user(7, "Augusta", "King", "augusta@example.com");
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(currentUser));
        Mockito.when(userService.updateProfile(Mockito.any(UserModel.class))).thenReturn(Optional.of(updatedUser));
        final ProfileForm form = validProfileForm();
        form.setGivenName(" Augusta ");
        form.setLastName(" King ");
        form.setEmail(" Augusta@Example.COM ");
        form.setPhone(" 123 ");
        form.setPaymentAlias(" pay.alias ");
        form.setPreferredLanguage("en");
        final BindingResult errors = new BeanPropertyBindingResult(form, "profileForm");

        final ModelAndView mav = profilePresentation.profileSubmit(form, errors);

        Assertions.assertEquals("redirect:/profile?profileAction=updated", mav.getViewName());
        Assertions.assertEquals(
                "augusta@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userService).updateProfile(userCaptor.capture());
        Assertions.assertEquals(7, userCaptor.getValue().getId());
        Assertions.assertEquals("Augusta", userCaptor.getValue().getGivenName());
        Assertions.assertEquals("King", userCaptor.getValue().getLastName());
        Assertions.assertEquals("Augusta@Example.COM", userCaptor.getValue().getEmail());
        Assertions.assertEquals(" 123 ", userCaptor.getValue().getPhone());
        Assertions.assertEquals(" pay.alias ", userCaptor.getValue().getPaymentAlias());
        Assertions.assertEquals(PreferredLanguageModel.EN, userCaptor.getValue().getPreferredLanguage());
    }

    @Test
    public void testProfilePasswordRecoveryDelegatesToService() {
        authenticate("ada@example.com");
        final UserModel user = user(7, "Ada", "Lovelace", "ada@example.com");
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        final ModelAndView mav = profilePresentation.profilePasswordRecoveryRequest();

        Assertions.assertEquals("redirect:/profile?passwordRecovery=sent", mav.getViewName());
        Mockito.verify(userService).requestPasswordRecovery(user);
    }

    private static void authenticate(final String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, "password", Collections.emptyList()));
    }

    private static ProfileForm validProfileForm() {
        final ProfileForm form = new ProfileForm();
        form.setGivenName("Ada");
        form.setLastName("Lovelace");
        form.setEmail("ada@example.com");
        form.setPhone("123");
        form.setPaymentAlias("alias");
        form.setPreferredLanguage("es");
        return form;
    }

    private static UserModel user(final Integer id, final String givenName, final String lastName, final String email) {
        final UserModel user = new UserModel();
        user.setId(id);
        user.setGivenName(givenName);
        user.setLastName(lastName);
        user.setEmail(email);
        return user;
    }
}
