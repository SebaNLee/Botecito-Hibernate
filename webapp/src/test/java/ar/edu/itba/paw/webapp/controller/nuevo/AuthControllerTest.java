package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import ar.edu.itba.paw.webapp.presentation.AuthModelMapper;
import ar.edu.itba.paw.webapp.presentation.AuthPresentation;
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

    private AuthController controller;

    @BeforeEach
    public void setUp() {
        final AuthPresentation authPresentation = new AuthPresentation(userService, new AuthModelMapper());
        controller = new AuthController(authPresentation);
    }

    @Test
    public void testLoginFormDelegatesThroughPresentation() {
        final LoginForm form = new LoginForm();
        form.setRegistered("true");

        final ModelAndView mav = controller.login(form, new MockHttpServletRequest());

        Assertions.assertEquals("login", mav.getViewName());
        Assertions.assertEquals(true, mav.getModel().get("registeredSuccess"));
    }

    @Test
    public void testLoginStoresSafeNextRedirectInSession() {
        final LoginForm form = new LoginForm();
        form.setNext("/marketplace");
        final MockHttpServletRequest request = new MockHttpServletRequest();

        controller.login(form, request);

        Assertions.assertEquals("/marketplace", request.getSession().getAttribute("POST_LOGIN_REDIRECT"));
    }

    @Test
    public void testLoginIgnoresUnsafeNextRedirect() {
        final LoginForm form = new LoginForm();
        form.setNext("https://evil.example.com/steal");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("POST_LOGIN_REDIRECT", "/old");

        controller.login(form, request);

        Assertions.assertNull(request.getSession().getAttribute("POST_LOGIN_REDIRECT"));
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
        Mockito.verifyNoInteractions(userService);
    }
}
