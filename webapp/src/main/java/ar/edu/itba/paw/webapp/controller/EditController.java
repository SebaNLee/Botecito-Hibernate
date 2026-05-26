package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.EditPresentation;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class EditController {

    private final EditPresentation editPresentation;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return editPresentation.buildDifficultyOptions();
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return editPresentation.maxGalleryImages();
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView bootstrapEdit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("itemId") final int itemId,
            final HttpServletRequest request) {
        return editPresentation.bootstrapEdit(user, itemId, request);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/details", method = RequestMethod.GET)
    public ModelAndView editStepOne(@PathVariable("itemId") final int itemId) {
        return editPresentation.editStepOne(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/details", method = RequestMethod.POST)
    public ModelAndView editStepOneSubmit(
            @PathVariable("itemId") final int itemId,
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        return editPresentation.editStepOneSubmit(itemId, form, errors);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView editStepTwo(@PathVariable("itemId") final int itemId) {
        return editPresentation.editStepTwo(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/availability", method = RequestMethod.POST)
    public ModelAndView editStepTwoSubmit(
            @PathVariable("itemId") final int itemId,
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class}) @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors) {
        return editPresentation.editStepTwoSubmit(itemId, form, errors);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/images", method = RequestMethod.GET)
    public ModelAndView editStepThree(@PathVariable("itemId") final int itemId) {
        return editPresentation.editStepThree(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/images", method = RequestMethod.POST)
    public ModelAndView editStepThreeSubmit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("itemId") final int itemId,
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class, PublishBoatForm.Step3Edit.class})
                    @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return editPresentation.editStepThreeSubmit(user, itemId, form, errors, redirectAttributes);
    }
}
