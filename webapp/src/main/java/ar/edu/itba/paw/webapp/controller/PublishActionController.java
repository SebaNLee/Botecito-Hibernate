package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.dto.BookingDecisionBatch;
import ar.edu.itba.paw.services.dto.EditConflictView;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PublishActionController {

    private final ItemService itemService;
    private final UserService userService;

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.GET)
    public ModelAndView editPublicationForm(
            @PathVariable("id") final int itemId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
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
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
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
        if (parsedPrice == null || parsedLocationOptionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }
        final int publicationPrice = parsedPrice;
        final int publicationLocationOptionId = parsedLocationOptionId;
        final MultipartFile file = form.getFile();
        final boolean hasNewPrimaryImage = file != null && !file.isEmpty();
        final byte[] primaryImageDataForChangeDetection = hasNewPrimaryImage ? new byte[] {0} : null;

        if (!itemService.hasPublicationChanges(
                itemId,
                form.getTitle() == null ? "" : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                publicationPrice,
                form.getDifficultyLevel(),
                publicationLocationOptionId,
                primaryImageDataForChangeDetection)) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(itemId);
        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        itemService.resolveEditConflict(itemId, buildDecisionBatch(activeBookings, request));

        final byte[] primaryImageData;
        if (hasNewPrimaryImage) {
            try {
                primaryImageData = file.getBytes();
            } catch (final IOException e) {
                errors.rejectValue("file", "editPublication.validation.image.read");
                return editPublicationModelAndView(item.get(), request);
            }
        } else {
            primaryImageData = null;
        }
        try {
            if (!itemService.updatePublicationForOwner(
                    itemId,
                    currentUser.getId(),
                    form.getTitle().trim(),
                    form.getDescription() == null ? "" : form.getDescription().trim(),
                    publicationPrice,
                    form.getDifficultyLevel(),
                    publicationLocationOptionId,
                    primaryImageData)) {
                errors.reject("publish.submit.persistenceError");
                return editPublicationModelAndView(item.get(), request);
            }
        } catch (final DataAccessException | IllegalStateException e) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        ToastSupport.success(redirectAttributes, "profile.publications.updated");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemService.setItemActiveForOwner(itemId, currentUser.getId(), false)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemService.setItemActiveForOwner(itemId, currentUser.getId(), true)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        try {
            if (!itemService.deleteItemByIdForOwner(itemId, currentUser.getId())) {
                if (!Boolean.TRUE.equals(item.get().getActive())) {
                    ToastSupport.error(redirectAttributes, "profile.publications.deleteBlockedByBookings");
                    return new ModelAndView("redirect:/my-boats#my-publications");
                }
                ToastSupport.error(redirectAttributes, "profile.publications.error");
                return new ModelAndView("redirect:/my-boats#my-publications");
            }
        } catch (final DataAccessException e) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
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
        return itemService.findItemByIdForOwner(itemId, currentUser.getId());
    }

    private ModelAndView editPublicationModelAndView(final Item item, final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("edit-publication");
        final EditConflictView conflictView = itemService.buildEditConflictView(item.getId());
        mav.addObject("item", item);
        mav.addObject("activeEditBookings", conflictView.getActiveBookings());
        mav.addObject("editBookingGuests", conflictView.getGuestNames());
        mav.addObject("editBookingStartLabels", conflictView.getStartLabels());
        mav.addObject("editBookingFriendlyDates", conflictView.getFriendlyDates());
        mav.addObject("editBookingFriendlyTimeRanges", conflictView.getFriendlyTimeRanges());
        mav.addObject("editBookingFriendlyPrices", conflictView.getFriendlyPrices());
        mav.addObject("editBookingStatusCodes", conflictView.getStatusCodes());
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    private static BookingDecisionBatch buildDecisionBatch(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        final List<BookingDecisionBatch.Decision> decisions = new ArrayList<>();
        for (final ItemBooking booking : activeBookings) {
            if (booking == null
                    || booking.getState() != BookingState.BOOKING_PENDING
                    || booking.getHostDecisionToken() == null) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if ("accept".equals(decision)) {
                decisions.add(new BookingDecisionBatch.Decision(
                        booking.getHostDecisionToken(), BookingState.BOOKING_CONFIRMED));
            } else if ("decline".equals(decision)) {
                decisions.add(new BookingDecisionBatch.Decision(
                        booking.getHostDecisionToken(), BookingState.BOOKING_REJECTED));
            }
        }
        return new BookingDecisionBatch(decisions);
    }

    private static boolean allPendingBookingsHaveDecisions(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        for (final ItemBooking booking : activeBookings) {
            if (booking == null || booking.getState() != BookingState.BOOKING_PENDING) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if (!"accept".equals(decision) && !"decline".equals(decision)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isConfirmedSnapshotEdit(final HttpServletRequest request) {
        return "true".equals(request.getParameter("confirmEditWithSnapshots"));
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
