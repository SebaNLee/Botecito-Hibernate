package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

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

    public ModelAndView publishStepThreeSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToPublish();
            }
            return publishImagesView();
        }
        return null;
    }

    public ModelAndView publishCreatedRedirect(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "settings.publications.created");
        return new ModelAndView("redirect:/my-boats#my-publications");
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
