package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.util.BookingDisplayFormatter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final UserService userService;
    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard() {
        return new ModelAndView("redirect:/my-boats");
    }

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("user", user);
        addMyBoatsData(
                mav,
                user,
                resolveBookingStatusFilters(status),
                normalizeQuery(query),
                sanitizePage(page),
                request.getContextPath());
        return mav;
    }

    @RequestMapping(value = "/bookings", method = RequestMethod.GET)
    public ModelAndView bookings(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("bookings");
        mav.addObject("user", user);
        addMyTripsData(
                mav,
                user,
                resolveBookingStatusFilters(status),
                normalizeQuery(query),
                sanitizePage(page),
                request.getContextPath());
        return mav;
    }

    @RequestMapping(value = "/profile/dashboard", method = RequestMethod.GET)
    public ModelAndView legacyDashboardRedirect() {
        return new ModelAndView("redirect:/my-boats");
    }

    private static final int DASHBOARD_PAGE_SIZE = 6;

    private void addMyBoatsData(
            final ModelAndView mav,
            final User user,
            final List<String> statuses,
            final String query,
            final int page,
            final String contextPath) {
        final List<Item> ownedItems = itemService.listItemsByOwnerId(user.getId());
        final Map<Integer, Integer> coverImageIdsByItemId = new LinkedHashMap<>();
        final Set<Integer> imageItemIds = new LinkedHashSet<>();
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            imageItemIds.add(item.getId());
            itemService
                    .findCoverImageIdByItemId(item.getId())
                    .ifPresent(imageId -> coverImageIdsByItemId.put(item.getId(), imageId));
        }
        final Page<ItemBooking> receivedBookingPage =
                paginate(itemService.listBookingsByOwnerId(user.getId()), page, DASHBOARD_PAGE_SIZE);
        final List<Map<String, Object>> receivedBookingCards = new ArrayList<>();
        final String ownerPaymentAlias = BookingDisplayFormatter.resolvePaymentAlias(user);
        for (final ItemBooking booking : receivedBookingPage.getContent()) {
            if (booking != null && booking.getItemId() != null) {
                imageItemIds.add(booking.getItemId());
            }
            if (booking == null || booking.getId() == null) {
                continue;
            }

            final Integer bookingId = booking.getId();
            final Item item = booking.getItemId() == null
                    ? null
                    : itemService.findItemById(booking.getItemId()).orElse(null);
            final User requester = booking.getGuestId() == null
                    ? null
                    : itemService.findUserById(booking.getGuestId()).orElse(null);
            final Integer pricePerHour = item == null ? null : item.getPricePerHour();
            final var paymentProof =
                    bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
            final String contentType = paymentProof == null ? null : paymentProof.getContentType();
            final String paymentRefusalReason = paymentProof == null ? null : paymentProof.getRefusalReason();
            final String paymentGuestReply = paymentProof == null ? null : paymentProof.getGuestReply();

            final Map<String, Object> bookingCard = new LinkedHashMap<>();
            bookingCard.put("id", bookingId);
            bookingCard.put("itemId", booking.getItemId());
            bookingCard.put("itemTitle", item == null ? "" : item.getTitle());
            bookingCard.put(
                    "statusMessageCode",
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
            bookingCard.put("dateLabel", BookingDisplayFormatter.formatDateLabel(booking.getStartTime()));
            bookingCard.put(
                    "timeRangeLabel",
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()));
            bookingCard.put(
                    "totalPriceLabel",
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            bookingCard.put("paymentAlias", ownerPaymentAlias);
            bookingCard.put("requestMessage", booking.getRequestMessage());
            bookingCard.put(
                    "hasRequestMessage",
                    booking.getRequestMessage() != null
                            && !booking.getRequestMessage().isBlank());
            bookingCard.put("requesterName", requester == null ? "" : requester.getName());
            bookingCard.put("requesterEmail", requester == null ? "" : requester.getEmail());
            bookingCard.put("requesterHasReviews", false);
            bookingCard.put("requesterAverageRating", 0);
            bookingCard.put("requesterTotalReviews", 0);
            bookingCard.put("hasPaymentGuestReply", paymentGuestReply != null && !paymentGuestReply.isBlank());
            bookingCard.put("paymentGuestReply", paymentGuestReply);
            bookingCard.put("hasPaymentProof", paymentProof != null && paymentProof.getFileData() != null);
            bookingCard.put("isPaymentProofPdf", "application/pdf".equalsIgnoreCase(contentType));
            bookingCard.put("paymentRefusalReason", paymentRefusalReason);
            bookingCard.put("hasPaymentRefusalReason", paymentRefusalReason != null && !paymentRefusalReason.isBlank());
            receivedBookingCards.add(bookingCard);
        }

        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", coverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", buildImageUrlsByItemId(imageItemIds, contextPath));
        mav.addObject("publicationDeleteDeactivatesByItemId", buildDeleteDeactivatesByItemId(ownedItems));
        mav.addObject("publicationDeleteDisabledByItemId", buildDeleteDisabledByItemId(ownedItems));
        mav.addObject("receivedBookingRequests", receivedBookingCards);
        mav.addObject("receivedBookingPage", receivedBookingPage);
        mav.addObject("selectedBookingStatusFilters", statuses);
        mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statuses));
        mav.addObject("boatSearchQuery", query);
        mav.addObject("pendingOwnerUserReviewsByBookingId", Map.of());
        mav.addObject("authoredUserReviewsByBookingId", Map.of());
    }

    private void addMyTripsData(
            final ModelAndView mav,
            final User user,
            final List<String> statuses,
            final String query,
            final int page,
            final String contextPath) {
        final Page<ItemBooking> sentBookingPage =
                paginate(itemService.listBookingsByGuestId(user.getId()), page, DASHBOARD_PAGE_SIZE);
        final Set<Integer> imageItemIds = new LinkedHashSet<>();
        final Map<Integer, String> sentStatusMessageCodeByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentDateLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTimeRangeLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTotalPriceLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentItemTitleByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerNameByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerEmailByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentAliasByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentHasPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentPaymentProofPdfByBookingId = new LinkedHashMap<>();
        final Map<Integer, Integer> sentBookedSnapshotVersionIdByBookingId = new LinkedHashMap<>();

        for (final ItemBooking booking : sentBookingPage.getContent()) {
            if (booking != null && booking.getItemId() != null) {
                imageItemIds.add(booking.getItemId());
            }
            if (booking == null || booking.getId() == null) {
                continue;
            }

            final Integer bookingId = booking.getId();
            final Item item = booking.getItemId() == null
                    ? null
                    : itemService.findItemById(booking.getItemId()).orElse(null);
            final User owner = item != null && item.getOwnerId() != null
                    ? itemService.findUserById(item.getOwnerId()).orElse(null)
                    : null;
            final Integer pricePerHour = item == null ? null : item.getPricePerHour();
            final var paymentProof =
                    bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
            final String contentType = paymentProof == null ? null : paymentProof.getContentType();
            final String refusalReason = paymentProof == null ? null : paymentProof.getRefusalReason();

            sentStatusMessageCodeByBookingId.put(
                    bookingId,
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
            sentDateLabelByBookingId.put(bookingId, BookingDisplayFormatter.formatDateLabel(booking.getStartTime()));
            sentTimeRangeLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()));
            sentTotalPriceLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            sentItemTitleByBookingId.put(bookingId, item == null ? "" : item.getTitle());
            sentOwnerNameByBookingId.put(bookingId, owner == null ? "" : owner.getName());
            sentOwnerEmailByBookingId.put(bookingId, owner == null ? "" : owner.getEmail());
            sentPaymentAliasByBookingId.put(
                    bookingId, owner == null ? "" : BookingDisplayFormatter.resolvePaymentAlias(owner));
            sentPaymentRefusalReasonByBookingId.put(bookingId, refusalReason);
            sentHasPaymentRefusalReasonByBookingId.put(bookingId, refusalReason != null && !refusalReason.isBlank());
            sentPaymentProofPdfByBookingId.put(bookingId, "application/pdf".equalsIgnoreCase(contentType));
            sentBookedSnapshotVersionIdByBookingId.put(bookingId, null);
        }
        mav.addObject("sentBookingRequests", sentBookingPage.getContent());
        mav.addObject("sentBookingPage", sentBookingPage);
        mav.addObject("imageUrlsByItemId", buildImageUrlsByItemId(imageItemIds, contextPath));
        mav.addObject("sentStatusMessageCodeByBookingId", sentStatusMessageCodeByBookingId);
        mav.addObject("sentDateLabelByBookingId", sentDateLabelByBookingId);
        mav.addObject("sentTimeRangeLabelByBookingId", sentTimeRangeLabelByBookingId);
        mav.addObject("sentTotalPriceLabelByBookingId", sentTotalPriceLabelByBookingId);
        mav.addObject("sentItemTitleByBookingId", sentItemTitleByBookingId);
        mav.addObject("sentOwnerNameByBookingId", sentOwnerNameByBookingId);
        mav.addObject("sentOwnerEmailByBookingId", sentOwnerEmailByBookingId);
        mav.addObject("sentPaymentAliasByBookingId", sentPaymentAliasByBookingId);
        mav.addObject("sentPaymentRefusalReasonByBookingId", sentPaymentRefusalReasonByBookingId);
        mav.addObject("sentHasPaymentRefusalReasonByBookingId", sentHasPaymentRefusalReasonByBookingId);
        mav.addObject("sentPaymentProofPdfByBookingId", sentPaymentProofPdfByBookingId);
        mav.addObject("sentBookedSnapshotVersionIdByBookingId", sentBookedSnapshotVersionIdByBookingId);
        mav.addObject("selectedBookingStatusFilters", statuses);
        mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statuses));
        mav.addObject("boatSearchQuery", query);
        mav.addObject("pendingGuestItemReviewsByBookingId", Map.of());
        mav.addObject("authoredItemReviewsByBookingId", Map.of());
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

    private Map<Integer, Boolean> buildDeleteDeactivatesByItemId(final List<Item> ownedItems) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasBlocking = itemService.listBookingsByItemId(item.getId()).stream()
                    .anyMatch(b -> b.getState() != BookingState.BOOKING_REJECTED
                            && b.getState() != BookingState.BOOKING_CANCELLED);
            map.put(item.getId(), Boolean.TRUE.equals(item.getActive()) && hasBlocking);
        }
        return map;
    }

    private Map<Integer, Boolean> buildDeleteDisabledByItemId(final List<Item> ownedItems) {
        final Map<Integer, Boolean> map = new LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        final OffsetDateTime now = OffsetDateTime.now();
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasFutureBlocking = itemService.listBookingsByItemId(item.getId()).stream()
                    .anyMatch(b -> b.getState() != BookingState.BOOKING_REJECTED
                            && b.getState() != BookingState.BOOKING_CANCELLED
                            && b.getEndTime() != null
                            && b.getEndTime().isAfter(now));
            map.put(item.getId(), !Boolean.TRUE.equals(item.getActive()) && hasFutureBlocking);
        }
        return map;
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

    private static <T> Page<T> paginate(final List<T> items, final int page, final int pageSize) {
        final int totalItems = items == null ? 0 : items.size();
        final int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        final int resolvedPage = totalPages == 0 ? 1 : Math.min(Math.max(1, page), totalPages);
        final int from = totalItems == 0 ? 0 : Math.min((resolvedPage - 1) * pageSize, totalItems);
        final int to = totalItems == 0 ? 0 : Math.min(from + pageSize, totalItems);
        return new Page<>(items == null ? List.of() : items.subList(from, to), resolvedPage, pageSize, totalItems);
    }
}
