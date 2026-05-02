package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private static final DateTimeFormatter BOOKING_START_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter BOOKING_FRIENDLY_RANGE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d 'of' MMMM, yyyy", Locale.ENGLISH);

    private final ItemService itemService;
    private final UserService userService;
    private final BookingRequestService bookingRequestService;

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
        final boolean hasPublicationChanges = hasPublicationChanges(
                item.get(),
                form.getTitle(),
                form.getDescription(),
                publicationPrice,
                form.getDifficultyLevel(),
                publicationLocationOptionId,
                hasNewPrimaryImage);

        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(itemId);
        if (!hasPublicationChanges) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }
        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }

        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        resolvePendingBookingsFromEditConflict(activeBookings, request);
        final byte[] primaryImageData;
        if (hasNewPrimaryImage) {
            try {
                final MultipartFile uploadedFile = form.getFile();
                if (uploadedFile == null) {
                    errors.rejectValue("file", "editPublication.validation.image.read");
                    return editPublicationModelAndView(item.get(), request);
                }
                primaryImageData = uploadedFile.getBytes();
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
        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(item.getId());
        final Map<Integer, String> guestNamesByBooking = buildGuestNamesByBooking(activeBookings);
        mav.addObject("item", item);
        mav.addObject("activeEditBookings", activeBookings);
        mav.addObject("editBookingGuests", guestNamesByBooking);
        mav.addObject("editBookingStartLabels", buildStartLabelsByBooking(activeBookings));
        mav.addObject("editBookingFriendlyDates", buildFriendlyBookingDatesByBooking(activeBookings));
        mav.addObject("editBookingFriendlyTimeRanges", buildFriendlyBookingTimeRangesByBooking(activeBookings));
        mav.addObject(
                "editBookingFriendlyPrices",
                buildFriendlyBookingPricesByBooking(activeBookings, item.getPricePerHour()));
        mav.addObject("editBookingStatusCodes", buildStatusCodesByBooking(activeBookings));
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    private void resolvePendingBookingsFromEditConflict(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        final List<String> tokensToAccept = new java.util.ArrayList<>();
        final List<String> tokensToDecline = new java.util.ArrayList<>();
        for (final ItemBooking booking : activeBookings) {
            if (booking == null
                    || booking.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_PENDING
                    || booking.getHostDecisionToken() == null) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if ("accept".equals(decision)) {
                tokensToAccept.add(booking.getHostDecisionToken());
            } else if ("decline".equals(decision)) {
                tokensToDecline.add(booking.getHostDecisionToken());
            }
        }
        bookingRequestService.resolveBookingRequests(
                tokensToAccept, ar.edu.itba.paw.models.BookingState.BOOKING_CONFIRMED);
        bookingRequestService.resolveBookingRequests(
                tokensToDecline, ar.edu.itba.paw.models.BookingState.BOOKING_REJECTED);
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
        final Map<Integer, String> namesByUserId = bookings.stream()
                .filter(booking -> booking != null && booking.getGuestId() != null)
                .map(ItemBooking::getGuestId)
                .distinct()
                .collect(Collectors.toMap(
                        guestId -> guestId,
                        guestId -> itemService
                                .findUserById(guestId)
                                .map(User::getName)
                                .orElse(""),
                        (left, right) -> left,
                        LinkedHashMap::new));
        for (final ItemBooking booking : bookings) {
            if (booking.getId() == null || booking.getGuestId() == null) {
                continue;
            }
            guestsByBooking.put(booking.getId(), namesByUserId.getOrDefault(booking.getGuestId(), ""));
        }
        return guestsByBooking;
    }

    private static boolean hasPublicationChanges(
            final Item item,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId,
            final boolean hasNewPrimaryImage) {
        if (item == null) {
            return true;
        }
        final String currentTitle =
                item.getTitle() == null ? "" : item.getTitle().trim();
        final String currentDescription =
                item.getDescription() == null ? "" : item.getDescription().trim();
        final String newTitle = title == null ? "" : title.trim();
        final String newDescription = description == null ? "" : description.trim();
        return hasNewPrimaryImage
                || !currentTitle.equals(newTitle)
                || !currentDescription.equals(newDescription)
                || item.getPricePerHour() == null
                || item.getPricePerHour() != pricePerHour
                || !java.util.Objects.equals(item.getDifficultyLevel(), difficultyLevel)
                || !java.util.Objects.equals(item.getLocationOptionId(), locationOptionId);
    }

    private static Map<Integer, String> buildFriendlyBookingDatesByBooking(final List<ItemBooking> bookings) {
        final Map<Integer, String> datesByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            datesByBooking.put(booking.getId(), formatFriendlyBookingDate(booking.getStartTime()));
        }
        return datesByBooking;
    }

    private static Map<Integer, String> buildFriendlyBookingTimeRangesByBooking(final List<ItemBooking> bookings) {
        final Map<Integer, String> rangesByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            rangesByBooking.put(
                    booking.getId(), formatFriendlyBookingTimeRange(booking.getStartTime(), booking.getEndTime()));
        }
        return rangesByBooking;
    }

    private static Map<Integer, String> buildFriendlyBookingPricesByBooking(
            final List<ItemBooking> bookings, final Integer pricePerHour) {
        final Map<Integer, String> pricesByBooking = new LinkedHashMap<>();
        for (final ItemBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            pricesByBooking.put(
                    booking.getId(),
                    formatFriendlyBookingTotal(booking.getStartTime(), booking.getEndTime(), pricePerHour));
        }
        return pricesByBooking;
    }

    private static String formatFriendlyBookingDate(final OffsetDateTime startTime) {
        if (startTime == null) {
            return "";
        }
        return BOOKING_FRIENDLY_RANGE_FORMATTER.format(startTime);
    }

    private static String formatFriendlyBookingTimeRange(final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " to "
                + endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static String formatFriendlyBookingTotal(
            final OffsetDateTime startTime, final OffsetDateTime endTime, final Integer pricePerHour) {
        if (startTime == null || endTime == null || pricePerHour == null || pricePerHour <= 0) {
            return "";
        }
        final long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return "";
        }
        final BigDecimal totalPrice = BigDecimal.valueOf(pricePerHour.longValue())
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(totalPrice);
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
