package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.services.PublishService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

    private final PublishService publishService;
    private final SelectorsService selectorsInterface;

    public ModelAndView publishStepOne() {
        return new ModelAndView("publish");
    }

    public ModelAndView publishStepOneSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }
        return new ModelAndView("redirect:/publish/availability");
    }

    public ModelAndView publishStepTwo() {
        final ModelAndView mav = new ModelAndView("publish-availability");
        PublishWizardMapping.addAvailabilityEditorData(mav, new PublishBoatForm());
        return mav;
    }

    public ModelAndView publishStepTwoSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToPublish();
            }
            final ModelAndView mav = new ModelAndView("publish-availability");
            PublishWizardMapping.addAvailabilityEditorData(mav, form);
            return mav;
        }

        return new ModelAndView("redirect:/publish/images");
    }

    public ModelAndView publishStepThree() {
        final ModelAndView mav = new ModelAndView("publish-images");
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    public ModelAndView publishStepThreeSubmit(
            final BotecitoUserDetails principal,
            final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToPublish();
            }
            return publishImagesView();
        }

        publishService.create(
                principal.getId(),
                form.getItemTypeId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                form.getPricePerHour(),
                form.getCapacity(),
                form.getWeight(),
                form.getDifficulty(),
                form.getLocationOptionId(),
                PublishWizardMapping.toAvailabilityWindows(form),
                PublishWizardMapping.toPublishImageUploads(form));

        ToastSupport.success(redirectAttributes, "settings.publications.created");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public Map<String, String> buildDifficultyOptions() {
        return selectorsInterface.getDifficultyOptions();
    }

    public int maxGalleryImages() {
        return PublishBoatForm.MAX_GALLERY_IMAGES;
    }

    private static ModelAndView publishImagesView() {
        final ModelAndView mav = new ModelAndView("publish-images");
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    private static ModelAndView redirectToPublish() {
        final RedirectView redirectView = new RedirectView("/publish", true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }
}
