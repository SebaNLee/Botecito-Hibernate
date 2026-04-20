package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishActionController {

    private final ItemService itemService;
    private final UserService userService;

    public PublishActionController(final ItemService itemService, final UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.GET)
    public ModelAndView editPublicationForm(@PathVariable("id") final int itemId, final HttpServletRequest request) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden");
        }

        final EditPublicationForm form = new EditPublicationForm();
        form.setTitle(item.getTitle());
        form.setDescription(item.getDescription());
        form.setPricePerHour(item.getPricePerHour() == null ? "" : String.valueOf(item.getPricePerHour()));
        form.setDifficultyLevel(item.getDifficultyLevel());
        form.setMarina(item.getLocationOptionId() == null ? "" : String.valueOf(item.getLocationOptionId()));

        final ModelAndView mav = new ModelAndView("edit-publication");
        mav.addObject("editForm", form);
        mav.addObject("item", item);
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.POST)
    public ModelAndView editPublicationSubmit(
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("editForm") final EditPublicationForm form,
            final BindingResult errors,
            final HttpServletRequest request) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden");
        }

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("edit-publication");
            mav.addObject("item", item);
            mav.addObject(
                    "itemImageUrl",
                    ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
            return mav;
        }

        // BACKEND: persist the edit by calling a new service method such as
        //   itemService.updatePublication(
        //       itemId,
        //       form.getTitle().trim(),
        //       form.getDescription() == null ? "" : form.getDescription().trim(),
        //       Integer.parseInt(form.getPricePerHour().trim()),
        //       form.getDifficultyLevel(),
        //       Integer.parseInt(form.getMarina().trim()));
        // This will require adding `updatePublication(...)` to ItemService /
        // ItemServiceImpl and a corresponding UPDATE statement in ItemJdbcDao.

        // BACKEND: when the owner uploads a new image, persist it via
        //   itemService.insertImage(itemId, form.getFile().getBytes())
        // and decide replacement semantics (delete previous rows in
        // item_media or keep history). Validate form.getFile().getContentType()
        // starts with "image/" before touching bytes.
        // if (form.getFile() != null && !form.getFile().isEmpty()) { ... }

        return new ModelAndView("redirect:/profile?publishAction=updated#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActive(itemId, false)) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/profile?publishAction=disabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActive(itemId, true)) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/profile?publishAction=enabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden#my-publications");
        }

        // BACKEND: call itemService.deleteItemById(itemId) (new method) which
        // must DELETE the row from `item`. Note: `item_availability` already has
        // ON DELETE CASCADE, but `item_booking` does NOT — decide whether to
        // cascade, block deletion when bookings exist, or null-out the FK.

        return new ModelAndView("redirect:/profile?publishAction=deleted#my-publications");
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private Optional<Item> resolveOwnedItem(final User currentUser, final int itemId) {
        return itemService.listItemsByOwnerId(currentUser.getId()).stream()
                .filter(item -> item.getId() == itemId)
                .findFirst();
    }
}
