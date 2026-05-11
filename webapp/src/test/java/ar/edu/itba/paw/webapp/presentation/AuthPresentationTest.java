package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.form.nuevo.LoginForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class AuthPresentationTest {

    @Mock
    private UserService userService;

    private AuthPresentation authPresentation;

    @BeforeEach
    public void setUp() {
        authPresentation = new AuthPresentation(userService, new AuthModelMapper());
    }

    @Test
    public void testLoginAddsRequestedFlags() {
        final LoginForm form = new LoginForm();
        form.setError("true");
        form.setLogout("true");
        form.setRegistered("true");
        form.setVerificationSent("true");
        form.setVerified("true");
        form.setVerificationInvalid("true");
        form.setUnverified("true");
        form.setLegacyToken("true");
        form.setPasswordRecovered("true");

        final ModelAndView mav = authPresentation.login(form);

        Assertions.assertEquals("login", mav.getViewName());
        Assertions.assertEquals(true, mav.getModel().get("loginError"));
        Assertions.assertEquals(true, mav.getModel().get("logoutSuccess"));
        Assertions.assertEquals(true, mav.getModel().get("registeredSuccess"));
        Assertions.assertEquals(true, mav.getModel().get("verificationSentSuccess"));
        Assertions.assertEquals(true, mav.getModel().get("emailVerifiedSuccess"));
        Assertions.assertEquals(true, mav.getModel().get("verificationInvalidError"));
        Assertions.assertEquals(true, mav.getModel().get("unverifiedError"));
        Assertions.assertEquals(true, mav.getModel().get("legacyTokenError"));
        Assertions.assertEquals(true, mav.getModel().get("passwordRecoveredSuccess"));
    }

    @Test
    public void testRegisterSubmitRedirectsToLoginWithVerificationSent() {
        final RegisterForm form = validRegisterForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(java.util.Locale.ENGLISH);
        form.setPreferredLanguage("en");
        form.setRequest(request);
        Mockito.when(userService.register(Mockito.any(UserModel.class), Mockito.eq("password123")))
                .thenReturn(UserService.RegistrationResult.SUCCESS);

        final ModelAndView mav = authPresentation.registerSubmit(form, errors);

        Assertions.assertEquals("redirect:/login?verificationSent=true", mav.getViewName());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userService).register(userCaptor.capture(), Mockito.eq("password123"));
        Assertions.assertEquals("Ada", userCaptor.getValue().getGivenName());
        Assertions.assertEquals("Lovelace", userCaptor.getValue().getLastName());
        Assertions.assertEquals("ada@example.com", userCaptor.getValue().getEmail());
        Assertions.assertEquals(
                "en", userCaptor.getValue().getPreferredLanguage().getPersistenceCode());
    }

    @Test
    public void testRegisterSubmitRejectsDuplicateEmail() {
        final RegisterForm form = validRegisterForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "registerForm");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(java.util.Locale.ENGLISH);
        form.setPreferredLanguage("en");
        form.setRequest(request);
        Mockito.when(userService.register(Mockito.any(UserModel.class), Mockito.eq("password123")))
                .thenReturn(UserService.RegistrationResult.EMAIL_ALREADY_EXISTS);

        final ModelAndView mav = authPresentation.registerSubmit(form, errors);

        Assertions.assertEquals("nuevo/register", mav.getViewName());
        Assertions.assertTrue(errors.hasFieldErrors("email"));
    }

    @Test
    public void testPasswordRecoveryRequestSubmitRedirectsAfterRequestingRecovery() {
        final PasswordRecoveryRequestForm form = new PasswordRecoveryRequestForm();
        form.setEmail("ada@example.com");

        final ModelAndView mav = authPresentation.passwordRecoveryRequestSubmit(form);

        Assertions.assertEquals("redirect:/password-recovery?sent=true", mav.getViewName());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userService).requestPasswordRecovery(userCaptor.capture());
        Assertions.assertEquals("ada@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    public void testPasswordRecoveryResetFormMarksValidToken() {
        final PasswordResetForm form = new PasswordResetForm();
        form.setToken("token");
        Mockito.when(userService.findByPasswordRecoveryToken("token"))
                .thenReturn(Optional.of(Mockito.mock(UserModel.class)));

        final ModelAndView mav = authPresentation.passwordRecoveryResetForm(form);

        Assertions.assertEquals("nuevo/password-recovery-reset", mav.getViewName());
        Assertions.assertEquals("token", mav.getModel().get("token"));
        Assertions.assertEquals(true, mav.getModel().get("tokenValid"));
    }

    @Test
    public void testPasswordRecoveryResetSubmitReturnsInvalidTokenViewWhenTokenIsMissing() {
        final PasswordResetForm form = validPasswordResetForm();
        form.setToken("missing-token");
        Mockito.when(userService.findByPasswordRecoveryToken("missing-token")).thenReturn(Optional.empty());

        final ModelAndView mav = authPresentation.passwordRecoveryResetSubmit(form);

        Assertions.assertEquals("nuevo/password-recovery-reset", mav.getViewName());
        Assertions.assertEquals("missing-token", mav.getModel().get("token"));
        Assertions.assertEquals(false, mav.getModel().get("tokenValid"));
    }

    @Test
    public void testPasswordRecoveryResetSubmitRedirectsAfterSuccessfulReset() {
        final PasswordResetForm form = validPasswordResetForm();
        form.setToken("token");
        Mockito.when(userService.findByPasswordRecoveryToken("token"))
                .thenReturn(Optional.of(Mockito.mock(UserModel.class)));
        Mockito.when(userService.resetPassword(Mockito.any(UserModel.class), Mockito.eq("new-password")))
                .thenReturn(UserService.PasswordRecoveryResult.SUCCESS);

        final ModelAndView mav = authPresentation.passwordRecoveryResetSubmit(form);

        Assertions.assertEquals("redirect:/login?passwordRecovered=true", mav.getViewName());
        final ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Mockito.verify(userService).resetPassword(userCaptor.capture(), Mockito.eq("new-password"));
        Assertions.assertEquals("token", userCaptor.getValue().getPasswordRecoveryToken());
    }

    @Test
    public void testVerifyEmailRedirectsAfterSuccess() {
        Mockito.when(userService.verifyEmail("token")).thenReturn(Optional.of(Mockito.mock(UserModel.class)));

        final ModelAndView mav = authPresentation.verifyEmail("token");

        Assertions.assertEquals("redirect:/login?verified=true", mav.getViewName());
    }

    @Test
    public void testVerifyEmailRedirectsAfterInvalidToken() {
        Mockito.when(userService.verifyEmail("missing")).thenReturn(Optional.empty());

        final ModelAndView mav = authPresentation.verifyEmail("missing");

        Assertions.assertEquals("redirect:/login?verificationInvalid=true", mav.getViewName());
    }

    private static RegisterForm validRegisterForm() {
        final RegisterForm form = new RegisterForm();
        form.setGivenName("Ada");
        form.setLastName("Lovelace");
        form.setEmail("ada@example.com");
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setPaymentAlias(null);
        return form;
    }

    private static PasswordResetForm validPasswordResetForm() {
        final PasswordResetForm form = new PasswordResetForm();
        form.setPassword("new-password");
        form.setConfirmPassword("new-password");
        return form;
    }
}
