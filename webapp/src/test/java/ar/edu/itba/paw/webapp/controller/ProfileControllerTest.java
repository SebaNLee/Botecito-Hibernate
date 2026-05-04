package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    @Mock
    private UserService userService;

    private ProfileController controller;

    @BeforeEach
    public void setUp() {
        controller = new ProfileController(userService);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testProfileSubmitUpdatesProfileAndRefreshesAuthenticatedEmail() {
        final User currentUser = new User();
        currentUser.setId(7);
        currentUser.setGivenName("Ada");
        currentUser.setLastName("Lovelace");
        currentUser.setEmail("ada@example.com");

        final User updatedUser = new User();
        updatedUser.setId(7);
        updatedUser.setGivenName("Augusta");
        updatedUser.setLastName("King");
        updatedUser.setEmail("augusta@example.com");

        final Authentication auth = new UsernamePasswordAuthenticationToken(
                "ada@example.com", "password123", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final ProfileForm form = new ProfileForm();
        form.setGivenName("Augusta");
        form.setLastName("King");
        form.setEmail("augusta@example.com");
        form.setPhone("123");
        form.setPaymentAlias("alias");
        form.setPreferredLanguage("en");
        final BindingResult errors = new BeanPropertyBindingResult(form, "profileForm");

        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(currentUser));
        Mockito.when(userService.updateProfile(7, "Augusta", "King", "augusta@example.com", "123", "alias", "en"))
                .thenReturn(Optional.of(updatedUser));

        final ModelAndView mav = controller.profileSubmit(form, errors);

        Assertions.assertEquals("redirect:/profile?profileAction=updated", mav.getViewName());
        Assertions.assertEquals(
                "augusta@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
