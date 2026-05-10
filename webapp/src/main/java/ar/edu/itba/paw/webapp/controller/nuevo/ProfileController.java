package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.ProfileForm;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfilePresentation profilePresentation;

    @RequestMapping(value = "/profile/password-recovery", method = RequestMethod.POST)
    public ModelAndView profilePasswordRecoveryRequest() {
        return profilePresentation.profilePasswordRecoveryRequest();
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile(
            @RequestParam(value = "edit", defaultValue = "false") final boolean edit,
            @ModelAttribute("profileForm") final ProfileForm form) {
        return profilePresentation.profile(edit, form);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ModelAndView profileSubmit(
            @Valid @ModelAttribute("profileForm") final ProfileForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            return profilePresentation.profileSubmit(form, errors);
        }
        return profilePresentation.profileSubmit(form, errors);
    }
}
