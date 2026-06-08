package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.util.ToastSupport;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class MyBoatsActionsPresentation {

    private MyBoatsActionsPresentation() {}

    public static ModelAndView disablePublicationResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "settings.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public static ModelAndView enablePublicationResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "settings.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public static ModelAndView hardDeletePublicationResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "settings.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }
}
