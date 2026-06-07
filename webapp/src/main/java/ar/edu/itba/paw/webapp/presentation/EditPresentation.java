package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class EditPresentation {

    public ModelAndView editStepOne(final Version version, final int itemId, final HttpServletRequest request) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final PublishBoatForm form = PublishWizardMapping.fromVersion(version);
        final ModelAndView mav = new ModelAndView("edit-details");
        mav.addObject("itemId", itemId);
        mav.addObject("versionId", version.getId());
        mav.addObject("publishForm", form);
        mav.addObject("editGalleryImages", PublishWizardMapping.buildEditGallerySeeds(version, contextPath));
        return mav;
    }

    public ModelAndView editStepOneSubmit(final int itemId, final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("edit-details");
            mav.addObject("itemId", itemId);
            mav.addObject("publishForm", form);
            return mav;
        }
        return new ModelAndView("redirect:/edit/" + itemId + "/availability");
    }

    public ModelAndView editStepTwo(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-availability");
        mav.addObject("itemId", itemId);
        PublishWizardMapping.addAvailabilityEditorData(mav, new PublishBoatForm());
        return mav;
    }

    public ModelAndView editStepTwoSubmit(final int itemId, final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToEditDetails(itemId);
            }
            final ModelAndView mav = new ModelAndView("edit-availability");
            mav.addObject("itemId", itemId);
            mav.addObject("publishForm", form);
            PublishWizardMapping.addAvailabilityEditorData(mav, form);
            return mav;
        }
        return new ModelAndView("redirect:/edit/" + itemId + "/images");
    }

    public ModelAndView editStepThree(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-images");
        mav.addObject("itemId", itemId);
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    public ModelAndView editStepThreeSubmit(final int itemId, final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToEditDetails(itemId);
            }
            return editImagesView(itemId);
        }
        return null;
    }

    public ModelAndView editSavedRedirect(final boolean updated, final RedirectAttributes redirectAttributes) {
        if (updated) {
            ToastSupport.success(redirectAttributes, "settings.publications.updated");
        } else {
            ToastSupport.info(redirectAttributes, "settings.publications.noChanges");
        }
        return new ModelAndView("redirect:/my-boats");
    }

    public int maxGalleryImages() {
        return PublishBoatForm.MAX_GALLERY_IMAGES;
    }

    private static ModelAndView editImagesView(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-images");
        mav.addObject("itemId", itemId);
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    private static ModelAndView redirectToEditDetails(final int itemId) {
        final RedirectView redirectView = new RedirectView("/edit/" + itemId + "/details", true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }
}
