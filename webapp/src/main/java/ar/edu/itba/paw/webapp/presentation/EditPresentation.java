package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

public final class EditPresentation {

    private EditPresentation() {}

    public static ModelAndView editDetailsView(
            final int itemId,
            final int versionId,
            final PublishBoatForm form,
            final List<EditGalleryImageSeed> gallerySeeds) {
        return new ModelAndView("edit-details")
                .addObject("itemId", itemId)
                .addObject("versionId", versionId)
                .addObject("publishForm", form)
                .addObject("editGalleryImages", gallerySeeds);
    }

    public static ModelAndView editStepOneErrors(final int itemId, final PublishBoatForm form) {
        return new ModelAndView("edit-details").addObject("itemId", itemId).addObject("publishForm", form);
    }

    public static ModelAndView editStepOneSuccess(final int itemId) {
        return redirect("/edit/" + itemId + "/availability");
    }

    public static ModelAndView editEntryRedirect(final int itemId) {
        return redirect("/edit/" + itemId + "/details");
    }

    public static ModelAndView editStepTwo(final int itemId, final Map<String, Boolean> enabledWeekdays) {
        return new ModelAndView("edit-availability")
                .addObject("itemId", itemId)
                .addObject("enabledWeekdays", enabledWeekdays);
    }

    public static ModelAndView editStepTwoErrors(
            final int itemId, final PublishBoatForm form, final Map<String, Boolean> enabledWeekdays) {
        return new ModelAndView("edit-availability")
                .addObject("itemId", itemId)
                .addObject("publishForm", form)
                .addObject("enabledWeekdays", enabledWeekdays);
    }

    public static ModelAndView editStepTwoSuccess(final int itemId) {
        return redirect("/edit/" + itemId + "/images");
    }

    public static ModelAndView redirectToEditDetails(final int itemId) {
        return redirect("/edit/" + itemId + "/details");
    }

    public static ModelAndView editStepThree(final int itemId) {
        return new ModelAndView("edit-images").addObject("itemId", itemId).addObject("galleryPreviewUrls", List.of());
    }

    public static ModelAndView editStepThreeErrors(final int itemId) {
        return new ModelAndView("edit-images").addObject("itemId", itemId).addObject("galleryPreviewUrls", List.of());
    }

    public static ModelAndView editSavedRedirect(final boolean updated, final RedirectAttributes redirectAttributes) {
        if (updated) {
            ToastSupport.success(redirectAttributes, "settings.publications.updated");
        } else {
            ToastSupport.info(redirectAttributes, "settings.publications.noChanges");
        }
        return redirect("/my-boats");
    }

    private static ModelAndView redirect(final String target) {
        final RedirectView redirectView = new RedirectView(target, true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }
}
