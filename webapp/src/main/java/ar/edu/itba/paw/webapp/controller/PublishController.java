package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.PublishService;
import ar.edu.itba.paw.services.SelectorsService;
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

    private final PublishService publishService;
    private final SelectorsService selectorsService;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return selectorsService.getDifficultyOptions();
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return PublishBoatForm.MAX_GALLERY_IMAGES;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne() {
        return PublishPresentation.publishStepOne();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return PublishPresentation.publishStepOneErrors();
        }
        return PublishPresentation.publishStepOneSuccess();
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.GET)
    public ModelAndView publishStepTwo(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return PublishPresentation.publishStepTwo(form.getEnabledWeekdaysModel());
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class}) @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishBoatForm.hasErrorsOutsideAvailability(errors)) {
                return PublishPresentation.redirectToPublish();
            }
            return PublishPresentation.publishStepTwoErrors(form, form.getEnabledWeekdaysModel());
        }
        return PublishPresentation.publishStepTwoSuccess();
    }

    @RequestMapping(value = "/publish/images", method = RequestMethod.GET)
    public ModelAndView publishStepThree() {
        return PublishPresentation.publishStepThree();
    }

    @RequestMapping(value = "/publish/images", method = RequestMethod.POST)
    public ModelAndView publishStepThreeSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class, PublishBoatForm.Step3.class})
                    @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            if (PublishBoatForm.hasErrorsOutsideAvailability(errors)) {
                return PublishPresentation.redirectToPublish();
            }
            return PublishPresentation.publishStepThreeErrors();
        }
        publishService.create(
                user.getId(),
                form.getItemTypeId(),
                form.getTitle(),
                form.getDescription(),
                form.getPricePerHour(),
                form.getCapacity(),
                form.getWeight(),
                form.getDifficulty(),
                form.getLocationOptionId(),
                form.getAvailabilityWindows(),
                form.getPublishImageUploads());
        return PublishPresentation.publishCreatedRedirect(redirectAttributes);
    }
}
