package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.PublishPresentation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PublishController {

    private final PublishPresentation publishPresentation;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return publishPresentation.buildDifficultyOptions();
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return publishPresentation.maxGalleryImages();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne() {
        return publishPresentation.publishStepOne();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        return publishPresentation.publishStepOneSubmit(form, errors);
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.GET)
    public ModelAndView publishStepTwo() {
        return publishPresentation.publishStepTwo();
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class}) @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors) {
        return publishPresentation.publishStepTwoSubmit(form, errors);
    }

    @RequestMapping(value = "/publish/images", method = RequestMethod.GET)
    public ModelAndView publishStepThree() {
        return publishPresentation.publishStepThree();
    }

    @RequestMapping(value = "/publish/images", method = RequestMethod.POST)
    public ModelAndView publishStepThreeSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class, PublishBoatForm.Step3.class})
                    @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return publishPresentation.publishStepThreeSubmit(user, form, errors, redirectAttributes);
    }
}
