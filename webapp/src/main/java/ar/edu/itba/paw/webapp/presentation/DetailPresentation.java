package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.ItemDetail;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.DetailService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
import ar.edu.itba.paw.webapp.util.AvailabilityJsonHelper;
import ar.edu.itba.paw.webapp.util.DetailAvailabilityPicker;
import ar.edu.itba.paw.webapp.util.MarketplaceReturnUrl;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class DetailPresentation {

    private final DetailService detailInterface;
    private final BookingService bookingInterface;
    private final UserService userService;
    private final ToastPresentation toastPresentation;

    /**
     * @param pathSnapshotVersionId when present, the requested listing version from
     *                              {@code /item/{id}/snapshot/{versionId}}
     *                              (requires authentication via
     *                              {@link ar.edu.itba.paw.webapp.config.WebAuthConfig}).
     * @return {@link ModelAndView} for the detail page, or a {@link RedirectView}
     *         to
     *         the canonical item URL when the path names the current published
     *         version.
     */
    public Object detailPage(
            final int itemId,
            final BotecitoUserDetails user,
            final HttpServletRequest request,
            final Optional<Long> pathSnapshotVersionId) {
        final String marketplaceBackHref = MarketplaceReturnUrl.marketplaceBackHref(request, null);

        final Optional<ItemDetail> nuevoDetail;
        if (user != null && pathSnapshotVersionId.isPresent()) {
            nuevoDetail = detailInterface.getItemDetail(itemId, user.getId(), pathSnapshotVersionId.get());
        } else {
            nuevoDetail = detailInterface.getItemDetail(itemId);
        }
        if (nuevoDetail.isEmpty()
                || nuevoDetail.get().getVersions() == null
                || nuevoDetail.get().getVersions().isEmpty()) {
            if (pathSnapshotVersionId.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return buildNuevoItemListingMissingView(itemId, marketplaceBackHref);
        }
        final ItemDetail itemDetail = nuevoDetail.get();
        final long currentVersionId = itemDetail.getVersions().get(0).getVersionId();
        final ItemDetail.VersionDetail displayPair;
        if (pathSnapshotVersionId.isEmpty()) {
            displayPair = itemDetail.getVersions().get(0);
        } else {
            final long requestedId = pathSnapshotVersionId.get();
            if (requestedId == currentVersionId) {
                return snapshotRedirectToCanonicalItem(itemId);
            }
            displayPair = itemDetail.getVersions().stream()
                    .filter(v -> v.getVersionId() == requestedId)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        final boolean viewingNonCurrentVersion = displayPair.getVersionId() != currentVersionId;
        final List<Long> visibleVersionIds =
                user != null ? detailInterface.getVisibleVersionIds(itemId, user.getId()) : List.of(currentVersionId);
        return buildNuevoItemDetailView(
                itemId,
                itemDetail,
                displayPair,
                currentVersionId,
                viewingNonCurrentVersion,
                visibleVersionIds,
                user,
                request,
                marketplaceBackHref);
    }

    /**
     * Same as {@link #detailPage(int, BotecitoUserDetails, HttpServletRequest, Optional)}
     * for canonical item URL, plus validation error toasts for the pre-booking form.
     */
    public Object detailPageWithPreBookingValidationErrors(
            final int itemId,
            final BotecitoUserDetails user,
            final HttpServletRequest request,
            final BindingResult errors) {
        final Object page = detailPage(itemId, user, request, Optional.empty());
        if (!(page instanceof ModelAndView)) {
            return page;
        }
        final ModelAndView mav = (ModelAndView) page;
        if (!"item-detail".equals(mav.getViewName())) {
            return page;
        }
        mergeValidationToasts(mav, errors);
        return mav;
    }

    public Object submitPreBooking(
            final BotecitoUserDetails user,
            final HttpServletRequest request,
            final int itemId,
            final PreBookingForm form,
            final RedirectAttributes redirectAttributes) {
        if (user == null) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.loginRequired");
            return contextRelativeRedirect("/login");
        }
        if (form.getVersionId() == null
                || !isVersionVisibleForViewer(
                        itemId, user.getId(), form.getVersionId().intValue())) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.invalidVersion");
            return redirectToItem(itemId);
        }

        bookingInterface.createBooking(
                form.getVersionId().intValue(),
                form.getDate(),
                form.getStartTime(),
                form.getEndTime(),
                form.getMessage(),
                user.getId());
        ToastSupport.success(redirectAttributes, "detail.preBooking.success");
        return redirectToItem(itemId);
    }

    private boolean isVersionVisibleForViewer(final int itemId, final int viewerUserId, final int versionId) {
        return detailInterface.getVisibleVersionIds(itemId, viewerUserId).contains((long) versionId);
    }

    private void mergeValidationToasts(final ModelAndView mav, final BindingResult errors) {
        final List<Map<String, String>> validationToasts = toastPresentation.validationToasts(errors, "detail");
        @SuppressWarnings("unchecked")
        final List<Map<String, String>> existing =
                (List<Map<String, String>>) mav.getModel().get("toasts");
        if (existing != null && !existing.isEmpty()) {
            final List<Map<String, String>> merged = new ArrayList<>(existing);
            merged.addAll(validationToasts);
            mav.addObject("toasts", merged);
        } else {
            mav.addObject("toasts", validationToasts);
        }
    }

    private static RedirectView redirectToItem(final int itemId) {
        final RedirectView view = new RedirectView("/item/" + itemId);
        view.setContextRelative(true);
        return view;
    }

    private ModelAndView buildNuevoItemListingMissingView(final int itemId, final String marketplaceBackHref) {
        final ModelAndView mav = new ModelAndView("item-detail");
        mav.addObject("itemListingMissing", true);
        mav.addObject("itemId", itemId);
        mav.addObject("marketplaceBackHref", marketplaceBackHref);
        mav.addObject("toasts", toastPresentation.errorCodeToasts("detail.item.notFound"));
        return mav;
    }

    private static RedirectView snapshotRedirectToCanonicalItem(final int itemId) {
        final RedirectView view = new RedirectView("/item/" + itemId);
        view.setContextRelative(true);
        return view;
    }

    private static RedirectView contextRelativeRedirect(final String path) {
        final RedirectView view = new RedirectView(path);
        view.setContextRelative(true);
        return view;
    }

    private ModelAndView buildNuevoItemDetailView(
            final int itemId,
            final ItemDetail itemDetail,
            final ItemDetail.VersionDetail displayPair,
            final long currentVersionId,
            final boolean viewingNonCurrentVersion,
            final List<Long> visibleVersionIds,
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final String marketplaceBackHref) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final int ownerId = displayPair.getHostId();
        final boolean isOwner = viewer != null && ownerId > 0 && ownerId == viewer.getId();

        final Users itemOwner =
                ownerId <= 0 ? null : userService.findById(ownerId).orElse(null);

        final List<Review> versionReviews = displayPair.getReviews() == null ? List.of() : displayPair.getReviews();

        final boolean isActive = displayPair.getStatus() == ItemStatusEnum.ACTIVE;

        final ModelAndView mav = new ModelAndView("item-detail");
        mav.addObject("itemListingMissing", false);
        mav.addObject("itemDetail", itemDetail);
        mav.addObject("item", displayPair);
        mav.addObject("currentVersionId", currentVersionId);
        mav.addObject("selectedVersionId", displayPair.getVersionId());
        mav.addObject("viewingNonCurrentVersion", viewingNonCurrentVersion);
        mav.addObject("showVersionSelector", visibleVersionIds.size() > 1);
        mav.addObject("visibleVersionIds", visibleVersionIds);
        mav.addObject("isOwner", isOwner);
        mav.addObject("viewer", viewer);
        mav.addObject("hideListingLiveVersionNavigation", viewingNonCurrentVersion && !isActive && !isOwner);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("itemOwner", itemOwner);
        mav.addObject("versionReviews", versionReviews);
        mav.addObject("itemImageUrl", primaryImageUrl(displayPair.getImages(), contextPath));
        mav.addObject("itemImageUrls", prefixImagePaths(displayPair.getImages(), contextPath));
        final String ownerName = itemOwner == null
                ? ""
                : ((itemOwner.getFirstName() == null
                                        ? ""
                                        : itemOwner.getFirstName().trim())
                                + " "
                                + (itemOwner.getLastName() == null
                                        ? ""
                                        : itemOwner.getLastName().trim()))
                        .trim();
        final String ownerDisplayName = ownerName.isBlank()
                ? (itemOwner == null || itemOwner.getEmail() == null ? "" : itemOwner.getEmail())
                : ownerName;
        mav.addObject("itemOwnerDisplayName", ownerDisplayName);
        mav.addObject(
                "ownerInitial",
                ownerDisplayName.isEmpty()
                        ? "I"
                        : ownerDisplayName.substring(0, 1).toUpperCase());

        mav.addObject("itemLocationSlug", "");
        mav.addObject("marketplaceBackHref", marketplaceBackHref);

        final PreBookingForm preBookingForm = new PreBookingForm();
        preBookingForm.setVersionId((int) displayPair.getVersionId());
        mav.addObject("preBookingForm", preBookingForm);

        final List<Availability> availabilityWindows =
                displayPair.getAvailabilityWindows() == null ? List.of() : displayPair.getAvailabilityWindows();
        final var builderData = DetailAvailabilityPicker.build(
                availabilityWindows, displayPair.getBookings(), displayPair.getVersionTimezone());
        final var detailData = new AvailabilityData(
                builderData.offeredDates(), builderData.occupiedDates(),
                builderData.offeredTimesByDate(), builderData.occupiedTimesByDate());
        AvailabilityJsonHelper.addAvailabilityPickerData(mav, "detail", detailData);
        final String listingTz = displayPair.getVersionTimezone();
        mav.addObject("detailListingTimezoneId", listingTz == null || listingTz.isBlank() ? "" : listingTz.trim());
        mav.addObject(
                "detailListingTodayIso",
                DetailAvailabilityPicker.listingCalendarToday(listingTz).format(DateTimeFormatter.ISO_LOCAL_DATE));
        mav.addObject(
                "detailListingMaxDateIso",
                DetailAvailabilityPicker.listingCalendarMaxInclusive(listingTz)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE));

        final boolean showPreBookingPanel = isActive
                && !isOwner
                && !viewingNonCurrentVersion
                && availabilityWindows.stream().anyMatch(this::isCompleteAvailabilityWindow);
        mav.addObject("showPreBookingPanel", showPreBookingPanel);

        return mav;
    }

    private boolean isCompleteAvailabilityWindow(final Availability w) {
        return w.getWeekday() != null
                && w.getStartTime() != null
                && w.getEndTime() != null
                && w.getEndTime().isAfter(w.getStartTime());
    }

    private static String primaryImageUrl(final List<String> images, final String contextPath) {
        if (images == null || images.isEmpty()) {
            return contextPath + "/css/boat-placeholder.svg";
        }
        return prefixPath(images.get(0), contextPath);
    }

    private static List<String> prefixImagePaths(final List<String> paths, final String contextPath) {
        if (paths == null || paths.isEmpty()) {
            return List.of(contextPath + "/css/boat-placeholder.svg");
        }
        return paths.stream().map(p -> prefixPath(p, contextPath)).toList();
    }

    private static String prefixPath(final String path, final String contextPath) {
        if (path == null || path.isBlank()) {
            return contextPath + "/css/boat-placeholder.svg";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return path.startsWith("/") ? contextPath + path : contextPath + "/" + path;
    }
}
