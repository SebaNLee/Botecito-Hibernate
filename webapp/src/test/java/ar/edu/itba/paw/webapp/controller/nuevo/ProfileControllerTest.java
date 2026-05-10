package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.ProfileForm;
import ar.edu.itba.paw.webapp.presentation.nuevo.ProfilePresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    @Mock
    private ProfilePresentation profilePresentation;

    private ProfileController controller;

    @BeforeEach
    public void setUp() {
        controller = new ProfileController(profilePresentation);
    }

    @Test
    public void testProfileDelegatesToPresentation() {
        final ProfileForm form = new ProfileForm();
        Mockito.when(profilePresentation.profile(true, form)).thenReturn(new ModelAndView("profile"));

        final ModelAndView mav = controller.profile(true, form);

        Assertions.assertEquals("profile", mav.getViewName());
        Mockito.verify(profilePresentation).profile(true, form);
    }

    @Test
    public void testProfileSubmitDelegatesWithBindingErrors() {
        final ProfileForm form = new ProfileForm();
        final BindingResult errors = new BeanPropertyBindingResult(form, "profileForm");
        errors.rejectValue("email", "profile.validation.email.invalid");
        Mockito.when(profilePresentation.profileSubmit(form, errors)).thenReturn(new ModelAndView("profile"));

        final ModelAndView mav = controller.profileSubmit(form, errors);

        Assertions.assertEquals("profile", mav.getViewName());
        Mockito.verify(profilePresentation).profileSubmit(form, errors);
    }

    @Test
    public void testProfilePasswordRecoveryDelegatesToPresentation() {
        Mockito.when(profilePresentation.profilePasswordRecoveryRequest())
                .thenReturn(new ModelAndView("redirect:/profile?passwordRecovery=sent"));

        final ModelAndView mav = controller.profilePasswordRecoveryRequest();

        Assertions.assertEquals("redirect:/profile?passwordRecovery=sent", mav.getViewName());
        Mockito.verify(profilePresentation).profilePasswordRecoveryRequest();
    }
}
