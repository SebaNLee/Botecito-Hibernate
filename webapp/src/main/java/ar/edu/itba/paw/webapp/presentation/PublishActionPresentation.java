package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.VersionNotFoundException;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class PublishActionPresentation {

    private final ItemService itemInterface;
    private final BookingService bookingInterface;
    private final UserService userService;

    public ModelAndView editPublicationForm(
            final BotecitoUserDetails principal,
            final int itemId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        final MyBoatsItem item = itemInterface.requireOwnedItem(itemId, principal.getId());

        final PublishBoatForm form = new PublishBoatForm();
        form.setTitle(item.getTitle());
        form.setDescription(item.getDescription());
        form.setPricePerHour(item.getPrice() == null ? "" : String.valueOf(item.getPrice()));
        form.setDifficultyLevel(item.getDifficulty());
        form.setLocationOptionId(item.getLocationId() == null ? "" : String.valueOf(item.getLocationId()));

        return editPublicationModelAndView(item, request).addObject("publishForm", form);
    }

    public ModelAndView editPublicationSubmit(
            final BotecitoUserDetails principal,
            final int itemId,
            final PublishBoatForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        final MyBoatsItem item = itemInterface.requireOwnedItem(itemId, principal.getId());

        final Integer parsedPrice =
                parseIntegerField(form.getPricePerHour(), "pricePerHour", "publish.validation.price.numeric", errors);
        final Integer parsedLocationOptionId = parseIntegerField(
                form.getLocationOptionId(), "locationOptionId", "publish.validation.location.invalid", errors);

        if (errors.hasErrors()) {
            return editPublicationModelAndView(item, request);
        }
        if (parsedPrice == null || parsedLocationOptionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item, request);
        }

        if (!hasPublicationChanges(item, form)) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        final Integer versionId = item.getVersionId();
        if (versionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item, request);
        }

        final List<Booking> activeBookings = bookingInterface.getBookingsForVersion(versionId).stream()
                .filter(b -> {
                    if (b.getStatus() == null) return false;
                    final String name = b.getStatus().name();
                    return !"REJECTED".equals(name) && !"CANCELLED".equals(name);
                })
                .toList();

        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item, request).addObject("showEditConflictModal", true);
        }
        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item, request).addObject("showEditConflictModal", true);
        }

        resolvePendingBookings(activeBookings, principal.getId(), request);

        try {
            itemInterface.createPublicationVersion(
                    itemId,
                    principal.getId(),
                    form.getTitle().trim(),
                    form.getDescription() == null ? "" : form.getDescription().trim(),
                    parsedPrice,
                    form.getDifficultyLevel(),
                    parsedLocationOptionId);
        } catch (final VersionNotFoundException e) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item, request);
        }

        // TODO edit image handling

        ToastSupport.success(redirectAttributes, "profile.publications.updated");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView disablePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        itemInterface.setItemActiveForOwner(itemId, principal.getId(), false);
        ToastSupport.success(redirectAttributes, "profile.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView enablePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        itemInterface.setItemActiveForOwner(itemId, principal.getId(), true);
        ToastSupport.success(redirectAttributes, "profile.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView hardDeletePublication(
            final BotecitoUserDetails principal, final int itemId, final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        final MyBoatsItem item = itemInterface.requireOwnedItem(itemId, principal.getId());

        if (!itemInterface.deleteMyBoatsItem(itemId, principal.getId())) {
            if (!Boolean.TRUE.equals(item.getActive())) {
                ToastSupport.error(redirectAttributes, "profile.publications.deleteBlockedByBookings");
                return new ModelAndView("redirect:/my-boats#my-publications");
            }
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    private ModelAndView editPublicationModelAndView(final MyBoatsItem item, final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("edit-publication");

        final Integer versionId = item.getVersionId();
        final List<Booking> activeBookings = versionId == null
                ? List.of()
                : bookingInterface.getBookingsForVersion(versionId).stream()
                        .filter(b -> {
                            if (b.getStatus() == null) return false;
                            final String name = b.getStatus().name();
                            return !"REJECTED".equals(name) && !"CANCELLED".equals(name);
                        })
                        .toList();

        final Map<Integer, String> guestNames = new LinkedHashMap<>();
        final Map<Integer, String> friendlyDates = new LinkedHashMap<>();
        final Map<Integer, String> friendlyTimeRanges = new LinkedHashMap<>();
        final Map<Integer, String> friendlyPrices = new LinkedHashMap<>();
        final Map<Integer, String> statusCodes = new LinkedHashMap<>();
        final Integer pricePerHour = item.getPrice();

        for (final Booking booking : activeBookings) {
            final int id = booking.getId();
            final int guestId = booking.getGuest() != null ? booking.getGuest().getId() : 0;

            if (guestId > 0) {
                guestNames.put(
                        id,
                        userService
                                .findById(guestId)
                                .map(u -> (u.getFirstName() != null ? u.getFirstName() : "") + " "
                                        + (u.getLastName() != null ? u.getLastName() : ""))
                                .orElse(""));
            }

            if (booking.getStart() != null) {
                friendlyDates.put(id, booking.getStart().format(FRIENDLY_DATE_FORMATTER));
            }
            if (booking.getStart() != null && booking.getEnd() != null) {
                friendlyTimeRanges.put(
                        id,
                        booking.getStart().format(TIME_FORMATTER) + " to "
                                + booking.getEnd().format(TIME_FORMATTER));
            }
            if (booking.getStart() != null && booking.getEnd() != null && pricePerHour != null && pricePerHour > 0) {
                final long minutes = java.time.Duration.between(booking.getStart(), booking.getEnd())
                        .toMinutes();
                if (minutes > 0) {
                    final java.math.BigDecimal total = java.math.BigDecimal.valueOf(pricePerHour.longValue())
                            .multiply(java.math.BigDecimal.valueOf(minutes))
                            .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                    final java.text.NumberFormat nf =
                            java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-AR"));
                    nf.setMinimumFractionDigits(0);
                    nf.setMaximumFractionDigits(2);
                    friendlyPrices.put(id, nf.format(total));
                }
            }
            statusCodes.put(id, resolveStatusMessageCode(booking.getStatus()));
        }

        mav.addObject("item", item);
        mav.addObject("activeEditBookings", activeBookings);
        mav.addObject("editBookingGuests", guestNames);
        mav.addObject("editBookingFriendlyDates", friendlyDates);
        mav.addObject("editBookingFriendlyTimeRanges", friendlyTimeRanges);
        mav.addObject("editBookingFriendlyPrices", friendlyPrices);
        mav.addObject("editBookingStatusCodes", statusCodes);
        final String imageUrl;
        if (item.getCoverImageId() != null) {
            imageUrl = request.getContextPath() + "/image/" + item.getCoverImageId();
        } else {
            imageUrl = request.getContextPath() + "/css/boat-placeholder.svg";
        }
        mav.addObject("itemImageUrl", imageUrl);
        return mav;
    }

    private static final DateTimeFormatter FRIENDLY_DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static String resolveStatusMessageCode(final BookingStatusEnum status) {
        if (status == null) {
            return "profile.sentBookings.status.unknown";
        }
        return switch (status) {
            case PENDING -> "profile.sentBookings.status.pending";
            case ACCEPTED -> "profile.sentBookings.status.confirmed";
            case REJECTED -> "profile.sentBookings.status.rejected";
            case CANCELLED -> "profile.sentBookings.status.cancelled";
            case FINISHED -> "profile.sentBookings.status.completed";
            case PAID -> "profile.sentBookings.status.paid";
            case CONFIRMED -> "profile.sentBookings.status.paymentSubmitted";
            case REFUSED -> "profile.sentBookings.status.paymentRefused";
        };
    }

    private static boolean hasPublicationChanges(final MyBoatsItem item, final PublishBoatForm form) {
        if (!Objects.equals(
                item.getTitle(), form.getTitle() == null ? "" : form.getTitle().trim())) {
            return true;
        }
        if (!Objects.equals(
                item.getDescription(),
                form.getDescription() == null ? "" : form.getDescription().trim())) {
            return true;
        }
        if (item.getPrice() == null || !Objects.equals(String.valueOf(item.getPrice()), form.getPricePerHour())) {
            return true;
        }
        if (!Objects.equals(item.getDifficulty(), form.getDifficultyLevel())) {
            return true;
        }
        if (item.getLocationId() == null
                || !Objects.equals(String.valueOf(item.getLocationId()), form.getLocationOptionId())) {
            return true;
        }
        return false;
    }

    private static boolean isConfirmedSnapshotEdit(final HttpServletRequest request) {
        return "true".equals(request.getParameter("confirmEditWithSnapshots"));
    }

    private static boolean allPendingBookingsHaveDecisions(
            final List<Booking> activeBookings, final HttpServletRequest request) {
        for (final Booking booking : activeBookings) {
            final String statusName =
                    booking.getStatus() != null ? booking.getStatus().name() : "";
            if (!"PENDING".equals(statusName)) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if (!"accept".equals(decision) && !"decline".equals(decision)) {
                return false;
            }
        }
        return true;
    }

    private void resolvePendingBookings(
            final List<Booking> activeBookings, final int ownerId, final HttpServletRequest request) {
        for (final Booking booking : activeBookings) {
            final String statusName =
                    booking.getStatus() != null ? booking.getStatus().name() : "";
            if (!"PENDING".equals(statusName)) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if ("accept".equals(decision)) {
                bookingInterface.acceptBooking(booking.getId(), ownerId);
            } else if ("decline".equals(decision)) {
                bookingInterface.rejectBooking(booking.getId(), ownerId);
            }
        }
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
}
