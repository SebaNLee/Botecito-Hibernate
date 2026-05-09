package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.ItemStatus;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.nuevo.DetailInterface;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerSupport;
import ar.edu.itba.paw.webapp.util.MarketplaceReturnUrl;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class DetailPresentation {

    private final DetailInterface detailInterface;
    private final ItemService itemService;
    private final UserService userService;
    private final ToastPresentation toastPresentation;

    public ModelAndView detailGet(
            final int itemId,
            final HttpServletRequest request,
            final ReservationRequestForm form,
            final String returnTo) {
        final String marketplaceBackHref = MarketplaceReturnUrl.marketplaceBackHref(request, returnTo);
        final String listingReturnTo = MarketplaceReturnUrl.listingReturnToForParam(returnTo);
        final Optional<ItemModel> nuevoItem = detailInterface.getItemById(itemId);
        if (nuevoItem.isEmpty()) {
            final ModelAndView mav = new ModelAndView("detail");
            mav.addObject("itemId", itemId);
            mav.addObject("resolvedItem", null);
            mav.addObject("showsCurrentPublishedVersion", false);
            mav.addObject("marketplaceBackHref", marketplaceBackHref);
            mav.addObject("listingReturnTo", listingReturnTo);
            mav.addObject("toasts", toastPresentation.errorCodeToasts("detail.item.notFound"));
            return mav;
        }
        return buildNuevoItemDetailView(itemId, nuevoItem.get(), request, form, marketplaceBackHref, listingReturnTo);
    }

    private ModelAndView buildNuevoItemDetailView(
            final int itemId,
            final ItemModel item,
            final HttpServletRequest request,
            final ReservationRequestForm form,
            final String marketplaceBackHref,
            final String listingReturnTo) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final Integer ownerId = parseHostId(item.getHostId());
        final User currentUser = currentAuthenticatedUserOrNull();
        final boolean isOwner = currentUser != null && ownerId != null && ownerId.equals(currentUser.getId());

        final User itemOwner =
                ownerId == null ? null : itemService.findUserById(ownerId).orElse(null);

        final List<Review> reviews = itemService.listLatestReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getReviewerUserId() == null || reviewAuthorNames.containsKey(review.getReviewerUserId())) {
                continue;
            }
            reviewAuthorNames.put(
                    review.getReviewerUserId(),
                    itemService
                            .findUserById(review.getReviewerUserId())
                            .map(User::getName)
                            .orElse(""));
        }

        final boolean isActive = item.getStatus() == ItemStatus.ACTIVE;
        final AvailabilityPickerBuilder.Data availability = AvailabilityPickerBuilder.build(
                itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));

        final ModelAndView mav = new ModelAndView("nuevo/item-detail");
        mav.addObject("item", item);
        mav.addObject("isOwner", isOwner);
        mav.addObject("selectedSnapshot", null);
        mav.addObject("hideListingLiveVersionNavigation", false);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("guestSnapshots", List.of());
        mav.addObject("hostSnapshots", List.of());
        mav.addObject("itemOwner", itemOwner);
        mav.addObject("itemReviews", reviews);
        mav.addObject("reviewAuthorNames", reviewAuthorNames);
        mav.addObject("reviewCreatedAtLabels", buildReviewCreatedAtLabels(reviews));
        mav.addObject("itemImageUrl", primaryImageUrl(item, contextPath));
        mav.addObject("itemImageUrls", prefixImagePaths(item.getImages(), contextPath));
        final String ownerName = itemOwner == null ? null : itemOwner.getName();
        mav.addObject(
                "ownerInitial",
                ownerName == null || ownerName.isEmpty()
                        ? "I"
                        : ownerName.substring(0, 1).toUpperCase());
        mav.addObject(
                "pendingItemReviewAction",
                currentUser == null
                        ? null
                        : itemService
                                .findPendingReviewAction(currentUser.getId(), itemId)
                                .orElse(null));

        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", availability);
        final String requestedDate = form.getDate();
        final String requestedStart = form.getStartTime();
        final String requestedEnd = form.getEndTime();
        final String resolvedDate =
                AvailabilityPickerBuilder.resolveSelectedDate(requestedDate, availability.offeredDates(), "");
        final List<String> reservationSlots = availability.offeredTimesByDate().getOrDefault(resolvedDate, List.of());
        final boolean validRequestedRange = resolvedDate.equals(requestedDate)
                && requestedStart != null
                && !requestedStart.isBlank()
                && requestedEnd != null
                && !requestedEnd.isBlank()
                && AvailabilityPickerBuilder.hasContinuousAvailability(reservationSlots, requestedStart, requestedEnd);
        final String resolvedStart = validRequestedRange
                ? requestedStart
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedStart, reservationSlots, "");
        final String resolvedEnd = validRequestedRange
                ? requestedEnd
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedEnd, reservationSlots, "");
        form.setDate(resolvedDate);
        form.setStartTime(resolvedStart);
        form.setEndTime(resolvedEnd);
        mav.addObject("reservationDate", resolvedDate);
        mav.addObject("reservationStartTime", resolvedStart);
        mav.addObject("reservationEndTime", resolvedEnd);
        mav.addObject("itemLocationSlug", "");
        mav.addObject("marketplaceBackHref", marketplaceBackHref);
        mav.addObject("listingReturnTo", listingReturnTo);
        return mav;
    }

    private static Integer parseHostId(final String hostId) {
        if (hostId == null || hostId.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(hostId.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
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

    private static Map<Integer, String> buildReviewCreatedAtLabels(final List<Review> reviews) {
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        final Map<Integer, String> labels = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getId() == null || review.getCreatedAt() == null) {
                continue;
            }
            labels.put(review.getId(), formatter.format(review.getCreatedAt()));
        }
        return labels;
    }

    private User currentAuthenticatedUserOrNull() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
