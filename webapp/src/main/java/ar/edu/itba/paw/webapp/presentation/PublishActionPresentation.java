package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.BookingStatusEnumOrm;
import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.services.BookingInterface;
import ar.edu.itba.paw.services.ItemInterface;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class PublishActionPresentation {

    private final ItemInterface itemInterface;
    private final BookingInterface bookingInterface;
    private final UserService userService;

    public ModelAndView editPublicationForm(
            final int itemId, final HttpServletRequest request, final RedirectAttributes redirectAttributes) {
        final UsersOrm currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        final PublishBoatForm form = new PublishBoatForm();
        form.setTitle(item.get().getTitle());
        form.setDescription(item.get().getDescription());
        form.setPricePerHour(
                item.get().getPrice() == null ? "" : String.valueOf(item.get().getPrice()));
        form.setDifficultyLevel(item.get().getDifficulty());
        form.setLocationOptionId(
                item.get().getLocationId() == null
                        ? ""
                        : String.valueOf(item.get().getLocationId()));

        return editPublicationModelAndView(item.get(), request).addObject("publishForm", form);
    }

    public ModelAndView editPublicationSubmit(
            final int itemId,
            final PublishBoatForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final UsersOrm currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        final Integer parsedPrice =
                parseIntegerField(form.getPricePerHour(), "pricePerHour", "publish.validation.price.numeric", errors);
        final Integer parsedLocationOptionId = parseIntegerField(
                form.getLocationOptionId(), "locationOptionId", "publish.validation.location.invalid", errors);

        if (errors.hasErrors()) {
            return editPublicationModelAndView(item.get(), request);
        }
        if (parsedPrice == null || parsedLocationOptionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        if (!hasPublicationChanges(item.get(), form)) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        final Integer versionId = item.get().getVersionId();
        if (versionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        final List<BookingOrm> activeBookings = bookingInterface.getBookingsForVersion(versionId).stream()
                .filter(b -> {
                    if (b.getStatus() == null) return false;
                    final String name = b.getStatus().name();
                    return !"REJECTED".equals(name) && !"CANCELLED".equals(name);
                })
                .toList();

        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }

        resolvePendingBookings(activeBookings, currentUser.getId(), request);

        final int newVersionId = itemInterface.createPublicationVersion(
                itemId,
                currentUser.getId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parsedPrice,
                form.getDifficultyLevel(),
                parsedLocationOptionId);
        if (newVersionId < 0) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        // TODO edit image handling

        ToastSupport.success(redirectAttributes, "profile.publications.updated");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView disablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UsersOrm currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (item.isEmpty() || !itemInterface.setItemActiveForOwner(itemId, currentUser.getId(), false)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView enablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UsersOrm currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (item.isEmpty() || !itemInterface.setItemActiveForOwner(itemId, currentUser.getId(), true)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView hardDeletePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UsersOrm currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
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

    private ModelAndView editPublicationModelAndView(final MyBoatsItem item, final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("edit-publication");

        final Integer versionId = item.getVersionId();
        final List<BookingOrm> activeBookings = versionId == null
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

        for (final BookingOrm booking : activeBookings) {
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

    private static String resolveStatusMessageCode(final BookingStatusEnumOrm status) {
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
            final List<BookingOrm> activeBookings, final HttpServletRequest request) {
        for (final BookingOrm booking : activeBookings) {
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
            final List<BookingOrm> activeBookings, final int ownerId, final HttpServletRequest request) {
        for (final BookingOrm booking : activeBookings) {
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

    private UsersOrm currentAuthenticatedUser() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
