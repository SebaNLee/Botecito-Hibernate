package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.services.nuevo.BookingInterface;
import ar.edu.itba.paw.services.nuevo.ItemInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.nuevo.EditPublicationForm;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;
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
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
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

    public ModelAndView editPublicationSubmit(
            final int itemId,
            final EditPublicationForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> item = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        validateUploadedImage(form.getFile(), errors);
        final Integer parsedPrice =
                parseIntegerField(form.getPricePerHour(), "pricePerHour", "publish.validation.price.numeric", errors);
        final Integer parsedLocationOptionId =
                parseIntegerField(form.getMarina(), "marina", "publish.validation.location.invalid", errors);

        if (errors.hasErrors()) {
            return editPublicationModelAndView(item.get(), request);
        }
        if (parsedPrice == null || parsedLocationOptionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        final boolean hasNewPrimaryImage =
                form.getFile() != null && !form.getFile().isEmpty();

        if (!hasPublicationChanges(item.get(), form, hasNewPrimaryImage)) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        final Integer versionId = item.get().getVersionId();
        if (versionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        final List<Booking> activeBookings = bookingInterface.getBookingsForVersion(versionId).stream()
                .filter(b -> b.getStatus() != BookingStatus.REJECTED && b.getStatus() != BookingStatus.CANCELLED)
                .toList();

        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }

        resolvePendingBookings(activeBookings, currentUser.getId(), request);

        final ItemUpdateModel update = new ItemUpdateModel();
        update.setTitle(form.getTitle().trim());
        update.setDescription(
                form.getDescription() == null ? "" : form.getDescription().trim());
        update.setPricePerHour(parsedPrice);
        update.setDifficultyLevel(form.getDifficultyLevel());
        update.setLocationOptionId(parsedLocationOptionId);

        final int newVersionId = itemInterface.createPublicationVersion(itemId, currentUser.getId(), update);
        if (newVersionId < 0) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        if (hasNewPrimaryImage) {
            final byte[] imageData = readPrimaryImageBytes(form.getFile(), errors);
            if (errors.hasErrors()) {
                return editPublicationModelAndView(item.get(), request);
            }
            if (imageData != null) {
                itemInterface.replaceVersionPrimaryImage(newVersionId, imageData);
            }
        }

        ToastSupport.success(redirectAttributes, "profile.publications.updated");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView disablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = currentAuthenticatedUser();
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
        final UserModel currentUser = currentAuthenticatedUser();
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
        final UserModel currentUser = currentAuthenticatedUser();
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
        final List<Booking> activeBookings = versionId == null
                ? List.of()
                : bookingInterface.getBookingsForVersion(versionId).stream()
                        .filter(b ->
                                b.getStatus() != BookingStatus.REJECTED && b.getStatus() != BookingStatus.CANCELLED)
                        .toList();

        final Map<Integer, String> guestNames = new LinkedHashMap<>();
        final Map<Integer, String> friendlyDates = new LinkedHashMap<>();
        final Map<Integer, String> friendlyTimeRanges = new LinkedHashMap<>();
        final Map<Integer, String> friendlyPrices = new LinkedHashMap<>();
        final Map<Integer, String> statusCodes = new LinkedHashMap<>();
        final Integer pricePerHour = item.getPricePerHour();

        for (final Booking booking : activeBookings) {
            final int id = booking.getId();

            if (booking.getGuestId() > 0) {
                guestNames.put(
                        id,
                        userService
                                .findById(booking.getGuestId())
                                .map(UserModel::getName)
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

    private static String resolveStatusMessageCode(final BookingStatus status) {
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

    private static boolean hasPublicationChanges(
            final MyBoatsItem item, final EditPublicationForm form, final boolean hasNewImage) {
        if (hasNewImage) {
            return true;
        }
        if (!Objects.equals(
                item.getTitle(), form.getTitle() == null ? "" : form.getTitle().trim())) {
            return true;
        }
        if (!Objects.equals(
                item.getDescription(),
                form.getDescription() == null ? "" : form.getDescription().trim())) {
            return true;
        }
        if (item.getPricePerHour() == null
                || !Objects.equals(String.valueOf(item.getPricePerHour()), form.getPricePerHour())) {
            return true;
        }
        if (!Objects.equals(item.getDifficultyLevel(), form.getDifficultyLevel())) {
            return true;
        }
        if (item.getLocationOptionId() == null
                || !Objects.equals(String.valueOf(item.getLocationOptionId()), form.getMarina())) {
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
            if (booking.getStatus() != BookingStatus.PENDING) {
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
            if (booking.getStatus() != BookingStatus.PENDING) {
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

    private static void validateUploadedImage(final MultipartFile file, final BindingResult errors) {
        if (file == null || file.isEmpty()) {
            return;
        }
        final String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            errors.rejectValue("file", "editPublication.validation.image.type");
        }
    }

    private static byte[] readPrimaryImageBytes(final MultipartFile uploadedFile, final BindingResult errors) {
        try {
            return uploadedFile.getBytes();
        } catch (final IOException e) {
            errors.rejectValue("file", "editPublication.validation.image.read");
            return null;
        }
    }

    private UserModel currentAuthenticatedUser() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
