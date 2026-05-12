package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.ItemInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class MyBoatsActionsPresentation {

    private final ItemInterface itemInterface;
    private final UserService userService;

    public ModelAndView disablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemInterface.setItemActiveForOwner(itemId, currentUser.getId(), false)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView enablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemInterface.setItemActiveForOwner(itemId, currentUser.getId(), true)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView hardDeletePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        if (!itemInterface.deleteMyBoatsItem(itemId, currentUser.getId())) {
            if (!Boolean.TRUE.equals(item.get().getActive())) {
                ToastSupport.error(redirectAttributes, "profile.publications.deleteBlockedByBookings");
                return new ModelAndView("redirect:/my-boats#my-publications");
            }
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    private Optional<MyBoatsItem> resolveOwnedItem(final UserModel currentUser, final int itemId) {
        return itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
    }

    private UserModel currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
