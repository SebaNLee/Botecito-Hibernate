package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

public final class PublishPresentation {

    private PublishPresentation() {}

    public static ModelAndView publishStepOne() {
        return new ModelAndView("publish");
    }

    public static ModelAndView publishStepOneErrors() {
        return new ModelAndView("publish");
    }

    public static ModelAndView publishStepOneSuccess() {
        return new ModelAndView("redirect:/publish/availability");
    }

    public static ModelAndView publishStepTwo(final Map<String, Boolean> enabledWeekdays) {
        return new ModelAndView("publish-availability").addObject("enabledWeekdays", enabledWeekdays);
    }

    public static ModelAndView publishStepTwoErrors(
            final PublishBoatForm form, final Map<String, Boolean> enabledWeekdays) {
        return new ModelAndView("publish-availability")
                .addObject("publishForm", form)
                .addObject("enabledWeekdays", enabledWeekdays);
    }

    public static ModelAndView publishStepTwoSuccess() {
        return new ModelAndView("redirect:/publish/images");
    }

    public static ModelAndView redirectToPublish() {
        final RedirectView redirectView = new RedirectView("/publish", true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }

    public static ModelAndView publishStepThree() {
        return new ModelAndView("publish-images").addObject("galleryPreviewUrls", List.of());
    }

    public static ModelAndView publishStepThreeErrors() {
        return new ModelAndView("publish-images").addObject("galleryPreviewUrls", List.of());
    }

    public static ModelAndView publishCreatedRedirect(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "settings.publications.created");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }
}
