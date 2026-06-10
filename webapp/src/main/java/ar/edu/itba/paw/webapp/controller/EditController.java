package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.PublishService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.EditGalleryImageSeed;
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

    private final ItemService itemService;
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

    @RequestMapping(value = "/edit/{itemId:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView editEntry(@PathVariable("itemId") final int itemId) {
        return EditPresentation.editEntryRedirect(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/details", method = RequestMethod.GET)
    public ModelAndView editStepOne(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("itemId") final int itemId,
            final HttpServletRequest request) {
        final var version = itemService.requireOwnedFullData(itemId, user.getId());
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final PublishBoatForm form = PublishBoatForm.fromVersion(version);
        final var gallerySeeds = EditGalleryImageSeed.fromVersion(version, contextPath);
        return EditPresentation.editDetailsView(itemId, version.getId(), form, gallerySeeds);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/details", method = RequestMethod.POST)
    public ModelAndView editStepOneSubmit(
            @PathVariable("itemId") final int itemId,
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return EditPresentation.editStepOneErrors(itemId, form);
        }
        return EditPresentation.editStepOneSuccess(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView editStepTwo(
            @PathVariable("itemId") final int itemId, @ModelAttribute("publishForm") final PublishBoatForm form) {
        return EditPresentation.editStepTwo(itemId, form.getEnabledWeekdaysModel());
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/availability", method = RequestMethod.POST)
    public ModelAndView editStepTwoSubmit(
            @PathVariable("itemId") final int itemId,
            @Validated({PublishBoatForm.Step1.class, PublishBoatForm.Step2.class}) @ModelAttribute("publishForm")
                    final PublishBoatForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishBoatForm.hasErrorsOutsideAvailability(errors)) {
                return EditPresentation.redirectToEditDetails(itemId);
            }
            return EditPresentation.editStepTwoErrors(itemId, form, form.getEnabledWeekdaysModel());
        }
        return EditPresentation.editStepTwoSuccess(itemId);
    }

    @RequestMapping(value = "/edit/{itemId:[0-9]+}/images", method = RequestMethod.GET)
    public ModelAndView editStepThree(@PathVariable("itemId") final int itemId) {
        return EditPresentation.editStepThree(itemId);
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
        if (errors.hasErrors()) {
            if (PublishBoatForm.hasErrorsOutsideAvailability(errors)) {
                return EditPresentation.redirectToEditDetails(itemId);
            }
            return EditPresentation.editStepThreeErrors(itemId);
        }
        final boolean updated = publishService.edit(
                itemId,
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
                form.getEditImageUploads());
        return EditPresentation.editSavedRedirect(updated, redirectAttributes);
    }
}
