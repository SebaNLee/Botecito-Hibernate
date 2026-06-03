package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.services.ManageItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class MyBoatsActionsPresentation {

    private final ManageItemService manageItemService;

    public ModelAndView disablePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {

        manageItemService.setEnabled(itemId, principal.getId(), false);
        ToastSupport.success(redirectAttributes, "settings.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView enablePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {

        manageItemService.setEnabled(itemId, principal.getId(), true);
        ToastSupport.success(redirectAttributes, "settings.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView hardDeletePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {

        manageItemService.deleteItem(itemId, principal.getId());
        ToastSupport.success(redirectAttributes, "settings.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }
}
