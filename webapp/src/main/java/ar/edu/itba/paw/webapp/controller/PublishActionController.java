package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
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

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden");
        }

        final EditPublicationForm form = new EditPublicationForm();
        form.setTitle(item.get().getTitle());
        form.setDescription(item.get().getDescription());
        form.setPricePerHour(
                item.get().getPricePerHour() == null
                        ? ""
                        : String.valueOf(item.get().getPricePerHour()));
        form.setDifficultyLevel(item.get().getDifficultyLevel());
        form.setMarina(
                item.get().getLocationOptionId() == null
                        ? ""
                        : String.valueOf(item.get().getLocationOptionId()));

        return editPublicationModelAndView(item.get(), request).addObject("editForm", form);
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

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden");
        }

        validateUploadedImage(form.getFile(), errors);
        final Integer parsedPrice =
                parseIntegerField(form.getPricePerHour(), "pricePerHour", "publish.validation.price.numeric", errors);
        final Integer parsedLocationOptionId =
                parseIntegerField(form.getMarina(), "marina", "publish.validation.location.invalid", errors);
        if (parsedLocationOptionId != null
                && itemService.listLocationOptions().stream()
                        .noneMatch(option -> parsedLocationOptionId.equals(option.getId()))) {
            errors.rejectValue("marina", "publish.validation.location.invalid");
        }

        if (errors.hasErrors()) {
            return editPublicationModelAndView(item.get(), request);
        }

        if (itemService.hasBlockingBookingsForEdition(itemId)) {
            errors.reject("editPublication.validation.blockedByBookings");
            return editPublicationModelAndView(item.get(), request);
        }

        final boolean updated = itemService.updatePublication(
                itemId,
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parsedPrice,
                form.getDifficultyLevel(),
                parsedLocationOptionId);
        if (!updated) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        final MultipartFile file = form.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                if (itemService.replacePrimaryImage(itemId, file.getBytes()) == null) {
                    errors.reject("publish.submit.persistenceError");
                    return editPublicationModelAndView(item.get(), request);
                }
            } catch (final IOException e) {
                errors.rejectValue("file", "editPublication.validation.image.read");
                return editPublicationModelAndView(item.get(), request);
            }
        }

        return new ModelAndView("redirect:/dashboard?publishAction=updated#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActive(itemId, false)) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/dashboard?publishAction=disabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActive(itemId, true)) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/dashboard?publishAction=enabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?publishAction=forbidden#my-publications");
        }

        if (itemService.hasBlockingBookingsForEdition(itemId)) {
            return new ModelAndView("redirect:/dashboard?publishAction=deleteBlockedByBookings#my-publications");
        }

        try {
            if (!itemService.deleteItemById(itemId)) {
                return new ModelAndView("redirect:/dashboard?publishAction=error#my-publications");
            }
        } catch (final DataAccessException e) {
            return new ModelAndView("redirect:/dashboard?publishAction=error#my-publications");
        }

        return new ModelAndView("redirect:/dashboard?publishAction=deleted#my-publications");
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

    private ModelAndView editPublicationModelAndView(final Item item, final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("edit-publication");
        mav.addObject("item", item);
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    private static Integer parseIntegerField(
            final String rawValue, final String fieldName, final String errorCode, final BindingResult errors) {
        if (rawValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (final NumberFormatException e) {
            errors.rejectValue(fieldName, errorCode);
            return null;
        }
    }

    private static void validateUploadedImage(final MultipartFile file, final BindingResult errors) {
        if (file == null || file.isEmpty()) {
            return;
        }
        final String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            errors.rejectValue("file", "editPublication.validation.image.type");
        }
    }
}
