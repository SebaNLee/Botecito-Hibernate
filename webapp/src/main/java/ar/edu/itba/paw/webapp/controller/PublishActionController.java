package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final DateTimeFormatter BOOKING_START_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ItemService itemService;
    private final UserService userService;
    private final BookingRequestService bookingRequestService;
    private final MailService mailService;

    public PublishActionController(
            final ItemService itemService,
            final UserService userService,
            final BookingRequestService bookingRequestService,
            final MailService mailService) {
        this.itemService = itemService;
        this.userService = userService;
        this.bookingRequestService = bookingRequestService;
        this.mailService = mailService;
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.GET)
    public ModelAndView editPublicationForm(@PathVariable("id") final int itemId, final HttpServletRequest request) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden");
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
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden");
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

        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(itemId);
        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }

        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        resolvePendingBookingsFromEditConflict(activeBookings, request);
        final byte[] primaryImageData;
        final MultipartFile file = form.getFile();
        if (file != null && !file.isEmpty()) {
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

        return new ModelAndView("redirect:/my-boats?publishAction=updated#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActiveForOwner(itemId, currentUser.getId(), false)) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/my-boats?publishAction=disabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden#my-publications");
        }

        if (!itemService.setItemActiveForOwner(itemId, currentUser.getId(), true)) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden#my-publications");
        }

        return new ModelAndView("redirect:/my-boats?publishAction=enabled#my-publications");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/my-boats?publishAction=forbidden#my-publications");
        }

        try {
            if (!itemService.deleteItemByIdForOwner(itemId, currentUser.getId())) {
                if (!Boolean.TRUE.equals(item.get().getActive())) {
                    return new ModelAndView("redirect:/my-boats?publishAction=deleteBlockedByBookings#my-publications");
                }
                return new ModelAndView("redirect:/my-boats?publishAction=error#my-publications");
            }
        } catch (final DataAccessException e) {
            return new ModelAndView("redirect:/my-boats?publishAction=error#my-publications");
        }

        return new ModelAndView("redirect:/my-boats?publishAction=deleted#my-publications");
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
        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(item.getId());
        mav.addObject("item", item);
        mav.addObject("activeEditBookings", activeBookings);
        mav.addObject("editBookingGuests", buildGuestNamesByBooking(activeBookings));
        mav.addObject("editBookingStartLabels", buildStartLabelsByBooking(activeBookings));
        mav.addObject("editBookingStatusCodes", buildStatusCodesByBooking(activeBookings));
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    private void resolvePendingBookingsFromEditConflict(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        for (final ItemBooking booking : activeBookings) {
            if (booking == null
                    || booking.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_PENDING
                    || booking.getHostDecisionToken() == null) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if ("accept".equals(decision)) {
                bookingRequestService
                        .resolveBookingRequest(
                                booking.getHostDecisionToken(), ar.edu.itba.paw.models.BookingState.BOOKING_CONFIRMED)
                        .ifPresent(mailService::sendBookingResolutionEmail);
            } else if ("decline".equals(decision)) {
                bookingRequestService
                        .resolveBookingRequest(
                                booking.getHostDecisionToken(), ar.edu.itba.paw.models.BookingState.BOOKING_REJECTED)
                        .ifPresent(mailService::sendBookingResolutionEmail);
            }
        }
    }

    private static boolean allPendingBookingsHaveDecisions(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        for (final ItemBooking booking : activeBookings) {
            if (booking == null || booking.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_PENDING) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if (!"accept".equals(decision) && !"decline".equals(decision)) {
                return false;
            }
        }
        return true;
    }

    private Map<Integer, String> buildGuestNamesByBooking(final List<ItemBooking> bookings) {
        final Map<Integer, String> guestsByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getId() == null || booking.getGuestId() == null) {
                continue;
            }
            guestsByBooking.put(
                    booking.getId(),
                    itemService
                            .findUserById(booking.getGuestId())
                            .map(User::getName)
                            .orElse(""));
        }
        return guestsByBooking;
    }

    private static boolean isConfirmedSnapshotEdit(final HttpServletRequest request) {
        return "true".equals(request.getParameter("confirmEditWithSnapshots"));
    }

    private Map<Integer, String> buildStartLabelsByBooking(final List<ItemBooking> bookings) {
        final Map<Integer, String> labelsByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getId() == null) {
                continue;
            }
            labelsByBooking.put(booking.getId(), toBookingStartLabel(booking.getStartTime()));
        }
        return labelsByBooking;
    }

    private Map<Integer, String> buildStatusCodesByBooking(final List<ItemBooking> bookings) {
        final Map<Integer, String> statusCodesByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getId() == null || booking.getState() == null) {
                continue;
            }
            statusCodesByBooking.put(
                    booking.getId(),
                    switch (booking.getState()) {
                        case BOOKING_PENDING -> "profile.sentBookings.status.pending";
                        case BOOKING_CONFIRMED -> "profile.sentBookings.status.confirmed";
                        case BOOKING_REJECTED -> "profile.sentBookings.status.rejected";
                        case BOOKING_CANCELLED -> "profile.sentBookings.status.cancelled";
                        case BOOKING_COMPLETED -> "profile.sentBookings.status.completed";
                        case BOOKING_PAYMENT_SUBMITTED -> "profile.sentBookings.status.paymentSubmitted";
                        case BOOKING_PAID -> "profile.sentBookings.status.paid";
                        case BOOKING_PAYMENT_REFUSED -> "profile.sentBookings.status.paymentRefused";
                    });
        }
        return statusCodesByBooking;
    }

    private static String toBookingStartLabel(final OffsetDateTime startTime) {
        if (startTime == null) {
            return "";
        }
        return BOOKING_START_LABEL_FORMATTER.format(startTime);
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
