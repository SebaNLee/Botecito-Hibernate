package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PostRegistrationAuthenticator postRegistrationAuthenticator;

    private AuthController controller;

    @BeforeEach
    public void setUp() {
        controller = new AuthController(userService, postRegistrationAuthenticator);
    }

    @Test
    public void testLoginFormDelegatesThroughPresentation() {
        final LoginForm form = new LoginForm();
        form.setRegistered("true");

        final ModelAndView mav = controller.login(form);

        Assertions.assertEquals("login", mav.getViewName());
        Assertions.assertEquals(true, mav.getModel().get("registeredSuccess"));
    }

    @Test
    public void testRegisterFormUsesNuevoView() {
        final ModelAndView mav = controller.registerForm(new RegisterForm());

        Assertions.assertEquals("nuevo/register", mav.getViewName());
    }

    @Test
    public void testRegisterSubmitReturnsNuevoViewWhenBindingHasErrors() {
        final RegisterForm form = new RegisterForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        errors.rejectValue("email", "register.validation.email.invalid");

        final ModelAndView mav = controller.registerSubmit(form, errors, new MockHttpServletRequest());

        Assertions.assertEquals("nuevo/register", mav.getViewName());
        Mockito.verifyNoInteractions(userService, postRegistrationAuthenticator);
    }
}
