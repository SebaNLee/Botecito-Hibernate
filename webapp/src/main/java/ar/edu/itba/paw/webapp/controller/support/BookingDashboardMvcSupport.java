package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingDashboardService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public final class BookingDashboardMvcSupport {

    private static final int DASHBOARD_PAGE_SIZE = 6;

    private final UserService userService;
    private final ItemService itemService;
    private final BookingDashboardService bookingDashboardService;

    public ModelAndView myBoats(
            final List<String> status, final String query, final int page, final HttpServletRequest request) {
        return currentUser()
                .map(user -> {
                    final ModelAndView mav = new ModelAndView("my-boats");
                    mav.addObject("user", user);
                    final var data = bookingDashboardService.loadOwnerBoatsDashboard(
                            user.getId(), sanitizePage(page), DASHBOARD_PAGE_SIZE);
                    final List<String> statusFilters = resolveBookingStatusFilters(status);
                    mav.addObject("ownedItems", data.getOwnedItems());
                    mav.addObject("publicationCoverImageIdsByItemId", data.getPublicationCoverImageIdsByItemId());
                    mav.addObject(
                            "imageUrlsByItemId",
                            buildImageUrlsByItemId(data.getImageItemIds(), request.getContextPath()));
                    mav.addObject(
                            "publicationDeleteDeactivatesByItemId", data.getPublicationDeleteDeactivatesByItemId());
                    mav.addObject("publicationDeleteDisabledByItemId", data.getPublicationDeleteDisabledByItemId());
                    mav.addObject("receivedBookingRequests", data.getReceivedBookingCards());
                    mav.addObject("receivedBookingPage", data.getReceivedBookingPage());
                    mav.addObject("receivedCanReviewByBookingId", data.getReceivedCanReviewByBookingId());
                    mav.addObject("selectedBookingStatusFilters", statusFilters);
                    mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statusFilters));
                    mav.addObject("boatSearchQuery", normalizeQuery(query));
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView guestBookings(
            final List<String> status, final String query, final int page, final HttpServletRequest request) {
        return currentUser()
                .map(user -> {
                    final ModelAndView mav = new ModelAndView("bookings");
                    mav.addObject("user", user);
                    final var data = bookingDashboardService.loadGuestTripsDashboard(
                            user.getId(), sanitizePage(page), DASHBOARD_PAGE_SIZE);
                    final List<String> statusFilters = resolveBookingStatusFilters(status);
                    mav.addObject(
                            "sentBookingRequests", data.getSentBookingPage().getContent());
                    mav.addObject("sentBookingPage", data.getSentBookingPage());
                    mav.addObject(
                            "imageUrlsByItemId",
                            buildImageUrlsByItemId(data.getImageItemIds(), request.getContextPath()));
                    mav.addObject("sentStatusMessageCodeByBookingId", data.getSentStatusMessageCodeByBookingId());
                    mav.addObject("sentDateLabelByBookingId", data.getSentDateLabelByBookingId());
                    mav.addObject("sentTimeRangeLabelByBookingId", data.getSentTimeRangeLabelByBookingId());
                    mav.addObject("sentTotalPriceLabelByBookingId", data.getSentTotalPriceLabelByBookingId());
                    mav.addObject("sentItemTitleByBookingId", data.getSentItemTitleByBookingId());
                    mav.addObject("sentOwnerNameByBookingId", data.getSentOwnerNameByBookingId());
                    mav.addObject("sentOwnerEmailByBookingId", data.getSentOwnerEmailByBookingId());
                    mav.addObject("sentPaymentAliasByBookingId", data.getSentPaymentAliasByBookingId());
                    mav.addObject("sentPaymentRefusalReasonByBookingId", data.getSentPaymentRefusalReasonByBookingId());
                    mav.addObject(
                            "sentHasPaymentRefusalReasonByBookingId", data.getSentHasPaymentRefusalReasonByBookingId());
                    mav.addObject("sentPaymentProofPdfByBookingId", data.getSentPaymentProofPdfByBookingId());
                    mav.addObject(
                            "sentBookedSnapshotVersionIdByBookingId", data.getSentBookedSnapshotVersionIdByBookingId());
                    mav.addObject("sentCanReviewByBookingId", data.getSentCanReviewByBookingId());
                    mav.addObject("selectedBookingStatusFilters", statusFilters);
                    mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statusFilters));
                    mav.addObject("boatSearchQuery", normalizeQuery(query));
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    private Optional<User> currentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }

    private static int sanitizePage(final int page) {
        return Math.max(1, page);
    }

    private static String normalizeQuery(final String query) {
        if (query == null) {
            return "";
        }
        return query.trim();
    }

    private static List<String> resolveBookingStatusFilters(final List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        final List<String> resolvedStatuses = new ArrayList<>();
        for (final String status : statuses) {
            if (isExactBookingStatusFilter(status) && !resolvedStatuses.contains(status)) {
                resolvedStatuses.add(status);
            }
        }
        return resolvedStatuses;
    }

    private static boolean isExactBookingStatusFilter(final String status) {
        return "pending".equals(status)
                || "confirmed".equals(status)
                || "paymentSubmitted".equals(status)
                || "paid".equals(status)
                || "paymentRefused".equals(status)
                || "completed".equals(status)
                || "rejected".equals(status)
                || "cancelled".equals(status);
    }

    private static Map<String, Boolean> buildSelectedStatusFilterMap(final List<String> statuses) {
        final Map<String, Boolean> selectedByStatus = new LinkedHashMap<>();
        for (final String status : List.of(
                "pending",
                "confirmed",
                "paymentSubmitted",
                "paid",
                "paymentRefused",
                "completed",
                "rejected",
                "cancelled")) {
            selectedByStatus.put(status, statuses != null && statuses.contains(status));
        }
        return selectedByStatus;
    }

    private Map<Integer, String> buildImageUrlsByItemId(final Set<Integer> itemIds, final String contextPath) {
        final Map<Integer, String> urls = new LinkedHashMap<>();
        if (itemIds == null) {
            return urls;
        }
        for (final Integer itemId : itemIds) {
            if (itemId == null) {
                continue;
            }
            urls.put(itemId, ItemImageUtils.resolveImageUrl(itemService, itemId, contextPath));
        }
        return urls;
    }
}
