package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.EditService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.EditPresentation;
import ar.edu.itba.paw.webapp.presentation.PublishWizardMapping;
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

    private final ItemService itemService;
    private final EditService editService;
    private final SelectorsService selectorsService;
    private final EditPresentation editPresentation;

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
        return editPresentation.maxGalleryImages();
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView bootstrapEdit(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("itemId") final int itemId,
            final HttpServletRequest request) {
        final var version = itemService.requireOwnedFullData(itemId, user.getId());
        return editPresentation.bootstrapEdit(version, itemId, request);
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
        final ModelAndView errorView = editPresentation.editStepThreeSubmit(itemId, form, errors);
        if (errorView != null) {
            return errorView;
        }
        final boolean updated = editService.edit(
                itemId,
                user.getId(),
                form.getItemTypeId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                form.getPricePerHour(),
                form.getCapacity(),
                form.getWeight(),
                form.getDifficulty(),
                form.getLocationOptionId(),
                PublishWizardMapping.toAvailabilityWindows(form),
                PublishWizardMapping.toEditImageUploads(form));
        return editPresentation.editSavedRedirect(updated, redirectAttributes);
    }
}
