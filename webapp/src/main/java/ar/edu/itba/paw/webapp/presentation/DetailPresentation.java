package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ItemDetail;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.models.nuevo.enums.ItemStatus;
import ar.edu.itba.paw.services.nuevo.BookingInterface;
import ar.edu.itba.paw.services.nuevo.DetailInterface;
import ar.edu.itba.paw.services.nuevo.PreBookingCreateResult;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.nuevo.PreBookingForm;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerSupport;
import ar.edu.itba.paw.webapp.util.MarketplaceReturnUrl;
import ar.edu.itba.paw.webapp.util.NuevoDetailAvailabilityPicker;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class DetailPresentation {

    private final DetailInterface detailInterface;
    private final BookingInterface bookingInterface;
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
            final int itemId, final HttpServletRequest request, final Optional<Long> pathSnapshotVersionId) {
        final String marketplaceBackHref = MarketplaceReturnUrl.marketplaceBackHref(request, null);
        final UserModel viewer = currentAuthenticatedUserOrNull();
        final Optional<Integer> viewerId = viewer == null ? Optional.empty() : Optional.of(viewer.getId());

        final Optional<ItemDetail> nuevoDetail = detailInterface.getItemDetail(itemId, viewerId, pathSnapshotVersionId);
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
        final ItemDetail.ItemModelVersion displayPair;
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
        return buildNuevoItemDetailView(
                itemId,
                itemDetail,
                displayPair,
                currentVersionId,
                viewingNonCurrentVersion,
                viewer,
                request,
                marketplaceBackHref);
    }

    /**
     * Same as {@link #detailPage(int, HttpServletRequest, Optional)} for canonical
     * item URL, plus
     * validation error toasts for the pre-booking form.
     */
    public Object detailPageWithPreBookingValidationErrors(
            final int itemId, final HttpServletRequest request, final BindingResult errors) {
        final Object page = detailPage(itemId, request, Optional.empty());
        if (!(page instanceof ModelAndView)) {
            return page;
        }
        final ModelAndView mav = (ModelAndView) page;
        if (!"nuevo/item-detail".equals(mav.getViewName())) {
            return page;
        }
        mergeValidationToasts(mav, errors);
        return mav;
    }

    public Object submitPreBooking(
            final HttpServletRequest request,
            final int itemId,
            final PreBookingForm form,
            final RedirectAttributes redirectAttributes) {
        final UserModel viewer = currentAuthenticatedUserOrNull();
        if (viewer == null) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.loginRequired");
            return contextRelativeRedirect("/login");
        }
        if (form.getVersionId() == null
                || !isVersionVisibleForViewer(
                        itemId, viewer.getId(), form.getVersionId().intValue())) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.invalidVersion");
            return redirectToItem(itemId);
        }

        final PreBookingReq req = new PreBookingReq();
        req.setVersionId(form.getVersionId().intValue());
        req.setDate(form.getDate());
        req.setStartTime(form.getStartTime());
        req.setEndTime(form.getEndTime());
        req.setMessage(form.getMessage());
        req.setGuestId(viewer.getId());

        final PreBookingCreateResult outcome = bookingInterface.createBooking(req);
        if (outcome instanceof PreBookingCreateResult.Created) {
            ToastSupport.success(redirectAttributes, "detail.preBooking.success");
            return redirectToItem(itemId);
        }
        if (outcome == PreBookingCreateResult.OutsideAvailability.INSTANCE) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.outsideAvailability");
            return redirectToItem(itemId);
        }
        if (outcome == PreBookingCreateResult.Collision.INSTANCE) {
            ToastSupport.error(redirectAttributes, "detail.preBooking.collision");
            return redirectToItem(itemId);
        }
        ToastSupport.error(redirectAttributes, "detail.preBooking.unexpected");
        return redirectToItem(itemId);
    }

    private boolean isVersionVisibleForViewer(final int itemId, final int viewerUserId, final int versionId) {
        final Optional<ItemDetail> detail =
                detailInterface.getItemDetail(itemId, Optional.of(viewerUserId), Optional.empty());
        if (detail.isEmpty()
                || detail.get().getVersions() == null
                || detail.get().getVersions().isEmpty()) {
            return false;
        }
        return detail.get().getVersions().stream().anyMatch(v -> v.getVersionId() == versionId);
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
        final ModelAndView mav = new ModelAndView("nuevo/item-detail");
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
            final ItemDetail.ItemModelVersion displayPair,
            final long currentVersionId,
            final boolean viewingNonCurrentVersion,
            final UserModel viewer,
            final HttpServletRequest request,
            final String marketplaceBackHref) {
        final ItemModel item = displayPair.getItemModel();
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final int ownerId = item.getHostId();
        final boolean isOwner = viewer != null && ownerId > 0 && ownerId == viewer.getId();

        final UserModel itemOwner =
                ownerId <= 0 ? null : userService.findById(ownerId).orElse(null);

        final List<ReviewModel> versionReviews =
                displayPair.getReviews() == null ? List.of() : displayPair.getReviews();

        final boolean isActive = item.getStatus() == ItemStatus.ACTIVE;

        final ModelAndView mav = new ModelAndView("nuevo/item-detail");
        mav.addObject("itemListingMissing", false);
        mav.addObject("itemDetail", itemDetail);
        mav.addObject("item", item);
        mav.addObject("currentVersionId", currentVersionId);
        mav.addObject("selectedVersionId", displayPair.getVersionId());
        mav.addObject("viewingNonCurrentVersion", viewingNonCurrentVersion);
        mav.addObject("showVersionSelector", itemDetail.getVersions().size() > 1);
        mav.addObject("isOwner", isOwner);
        mav.addObject("viewer", viewer);
        mav.addObject("hideListingLiveVersionNavigation", viewingNonCurrentVersion && !isActive && !isOwner);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("itemOwner", itemOwner);
        mav.addObject("versionReviews", versionReviews);
        mav.addObject("itemImageUrl", primaryImageUrl(item, contextPath));
        mav.addObject("itemImageUrls", prefixImagePaths(item.getImages(), contextPath));
        final String ownerName = itemOwner == null ? null : itemOwner.getName();
        mav.addObject(
                "ownerInitial",
                ownerName == null || ownerName.isEmpty()
                        ? "I"
                        : ownerName.substring(0, 1).toUpperCase());

        mav.addObject("itemLocationSlug", "");
        mav.addObject("marketplaceBackHref", marketplaceBackHref);

        final PreBookingForm preBookingForm = new PreBookingForm();
        preBookingForm.setVersionId((int) displayPair.getVersionId());
        mav.addObject("preBookingForm", preBookingForm);

        final List<AvailabilityWindow> availabilityWindows =
                displayPair.getAvailabilityWindows() == null ? List.of() : displayPair.getAvailabilityWindows();
        final AvailabilityPickerBuilder.Data detailPickerData = NuevoDetailAvailabilityPicker.build(
                availabilityWindows, displayPair.getBookings(), displayPair.getVersionTimezone());
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "detail", detailPickerData);
        final String listingTz = displayPair.getVersionTimezone();
        mav.addObject("detailListingTimezoneId", listingTz == null || listingTz.isBlank() ? "" : listingTz.trim());
        mav.addObject(
                "detailListingTodayIso",
                NuevoDetailAvailabilityPicker.listingCalendarToday(listingTz).format(DateTimeFormatter.ISO_LOCAL_DATE));
        mav.addObject(
                "detailListingMaxDateIso",
                NuevoDetailAvailabilityPicker.listingCalendarMaxInclusive(listingTz)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE));

        final boolean showPreBookingPanel = isActive
                && !isOwner
                && !viewingNonCurrentVersion
                && availabilityWindows.stream().anyMatch(this::isCompleteAvailabilityWindow);
        mav.addObject("showPreBookingPanel", showPreBookingPanel);

        return mav;
    }

    private boolean isCompleteAvailabilityWindow(final AvailabilityWindow w) {
        return w.getWeekday() != null
                && w.getStartTime() != null
                && w.getEndTime() != null
                && w.getEndTime().isAfter(w.getStartTime());
    }

    private static String primaryImageUrl(final ItemModel item, final String contextPath) {
        if (item.getImages() == null || item.getImages().isEmpty()) {
            return contextPath + "/css/boat-placeholder.svg";
        }
        return prefixPath(item.getImages().get(0), contextPath);
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

    private UserModel currentAuthenticatedUserOrNull() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
