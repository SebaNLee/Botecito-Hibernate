package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @Mock
    private BookingRequestService bookingRequestService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthController controller;

    @BeforeEach
    public void setUp() {
        controller = new AuthController(userService, new PostRegistrationAuthenticator(authenticationManager));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static RegisterForm validForm() {
        final RegisterForm form = new RegisterForm();
        form.setGivenName("Ada");
        form.setLastName("Lovelace");
        form.setEmail("ada@example.com");
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setPaymentAlias(null);
        return form;
    }

    @Test
    public void testRegisterSubmitAuthenticatesViaAuthenticationManagerOnSuccess() {
        final RegisterForm form = validForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(java.util.Locale.ENGLISH);

        Mockito.when(userService.register("Ada", "Lovelace", "ada@example.com", "password123", null, "en"))
                .thenReturn(UserService.RegistrationResult.SUCCESS);

        final Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "ada@example.com", "password123", java.util.Collections.emptyList());
        Mockito.when(authenticationManager.authenticate(Mockito.any(Authentication.class)))
                .thenReturn(authenticated);

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("redirect:/", mav.getViewName());
        Assertions.assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testRegisterSubmitFallsBackToLoginWhenAuthenticationFails() {
        final RegisterForm form = validForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(java.util.Locale.ENGLISH);

        Mockito.when(userService.register("Ada", "Lovelace", "ada@example.com", "password123", null, "en"))
                .thenReturn(UserService.RegistrationResult.SUCCESS);
        Mockito.when(authenticationManager.authenticate(Mockito.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("nope"));

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("redirect:/login?registered=true", mav.getViewName());
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testRegisterSubmitRejectsDuplicateEmailWithoutAuthenticating() {
        final RegisterForm form = validForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(java.util.Locale.ENGLISH);

        Mockito.when(userService.register("Ada", "Lovelace", "ada@example.com", "password123", null, "en"))
                .thenReturn(UserService.RegistrationResult.EMAIL_ALREADY_EXISTS);

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("register", mav.getViewName());
        Assertions.assertTrue(errors.hasFieldErrors("email"));
    }

    @Test
    public void testRegisterFormReturnsRegisterViewWhenUserAlreadyAuthenticated() {
        final Authentication auth = new UsernamePasswordAuthenticationToken(
                "ada@example.com", "password123", java.util.Collections.emptyList());

        SecurityContextHolder.getContext().setAuthentication(auth);

        final ModelAndView mav = controller.registerForm(new RegisterForm());

        Assertions.assertEquals("register", mav.getViewName());
    }

    @Test
    public void testRegisterSubmitRejectsPasswordMismatch() {
        final RegisterForm form = validForm();
        form.setConfirmPassword("different-password");
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("register", mav.getViewName());
        Assertions.assertTrue(errors.hasFieldErrors("confirmPassword"));
        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testPasswordRecoveryResetSubmitReturnsInvalidTokenViewWhenTokenIsMissing() {
        final PasswordResetForm form = new PasswordResetForm();
        form.setPassword("new-password");
        form.setConfirmPassword("new-password");
        final BindingResult errors = new BeanPropertyBindingResult(form, "passwordResetForm");

        Mockito.when(userService.findByPasswordRecoveryToken("missing-token")).thenReturn(Optional.empty());

        final ModelAndView mav = controller.passwordRecoveryResetSubmit("missing-token", form, errors);

        Assertions.assertEquals("password-recovery-reset", mav.getViewName());
        Assertions.assertEquals("missing-token", mav.getModel().get("token"));
        Assertions.assertEquals(false, mav.getModel().get("tokenValid"));
    }
}
