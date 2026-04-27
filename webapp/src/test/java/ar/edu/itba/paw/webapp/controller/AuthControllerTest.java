package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.RegisterForm;
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
    private MailService mailService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthController controller;

    @BeforeEach
    public void setUp() {
        controller = new AuthController(
                userService, itemService, bookingRequestService, mailService, reviewService, authenticationManager);
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

        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        Mockito.when(userService.register("Ada", "Lovelace", "ada@example.com", "password123", null))
                .thenReturn(new User());

        final Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "ada@example.com", "password123", java.util.Collections.emptyList());
        Mockito.when(authenticationManager.authenticate(Mockito.any(Authentication.class)))
                .thenReturn(authenticated);

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("redirect:/", mav.getViewName());

        final ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        Mockito.verify(authenticationManager).authenticate(captor.capture());
        final Authentication submitted = captor.getValue();
        Assertions.assertTrue(submitted instanceof UsernamePasswordAuthenticationToken);
        Assertions.assertEquals("ada@example.com", submitted.getPrincipal());
        Assertions.assertEquals("password123", submitted.getCredentials());
        Assertions.assertFalse(submitted.isAuthenticated());

        Assertions.assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testRegisterSubmitFallsBackToLoginWhenAuthenticationFails() {
        final RegisterForm form = validForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();

        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        Mockito.when(userService.register("Ada", "Lovelace", "ada@example.com", "password123", null))
                .thenReturn(new User());
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

        final User existing = new User();
        existing.setPasswordHash("already-set");
        Mockito.when(userService.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));

        final ModelAndView mav = controller.registerSubmit(form, errors, request);

        Assertions.assertEquals("register", mav.getViewName());
        Assertions.assertTrue(errors.hasFieldErrors("email"));
        Mockito.verifyNoInteractions(authenticationManager);
        Mockito.verify(userService, Mockito.never())
                .register(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());
    }
}
