package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.models.nuevo.PageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.nuevo.ItemInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.services.util.BookingDisplayFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MyBoatsPresentation {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int RECEIVED_PAGE_SIZE = 6;
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private final UserService userService;
    private final ItemInterface itemInterface;
    private final ItemService legacyItemService;

    public ModelAndView myBoats(final HttpServletRequest request, final int page, final int pageSize) {
        final UserModel currentUser = currentUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final int safePage = Math.max(1, page);
        final int safePageSize = Math.max(1, pageSize);
        final int totalItems = itemInterface.countMyBoatsItemsByOwnerId(currentUser.getId());
        final List<MyBoatsItem> ownedItems =
                itemInterface.listMyBoatsItemsByOwnerId(currentUser.getId(), safePage, safePageSize);
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();

        final Map<Integer, Integer> publicationCoverImageIdsByItemId = new LinkedHashMap<>();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();
        final Map<Integer, Boolean> publicationDeleteDeactivatesByItemId = new LinkedHashMap<>();
        final Map<Integer, Boolean> publicationDeleteDisabledByItemId = new LinkedHashMap<>();

        for (final MyBoatsItem item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (item.getCoverImageId() != null) {
                publicationCoverImageIdsByItemId.put(item.getId(), item.getCoverImageId());
                imageUrlsByItemId.put(item.getId(), contextPath + IMAGE_PATH_PREFIX + item.getCoverImageId());
            } else {
                imageUrlsByItemId.put(item.getId(), contextPath + PLACEHOLDER_IMAGE_PATH);
            }
            publicationDeleteDeactivatesByItemId.put(item.getId(), Boolean.TRUE.equals(item.getDeleteDeactivates()));
            publicationDeleteDisabledByItemId.put(item.getId(), Boolean.TRUE.equals(item.getDeleteDisabled()));
        }

        final List<ItemBooking> rawReceived = legacyItemService.listBookingsByOwnerId(currentUser.getId());
        final int totalReceived = rawReceived.size();
        final int fromIndex = Math.min((safePage - 1) * RECEIVED_PAGE_SIZE, totalReceived);
        final int toIndex = Math.min(fromIndex + RECEIVED_PAGE_SIZE, totalReceived);
        final List<ItemBooking> pageReceived = rawReceived.subList(fromIndex, toIndex);

        final List<Map<String, Object>> receivedBookingRequests = new ArrayList<>();
        for (final ItemBooking booking : pageReceived) {
            final Map<String, Object> req = new LinkedHashMap<>();
            req.put("id", booking.getId());
            req.put("itemId", booking.getItemId());

            final String itemTitle = legacyItemService
                    .findItemById(booking.getItemId())
                    .map(Item::getTitle)
                    .orElse("");
            req.put("itemTitle", itemTitle);

            final Integer pricePerHour = legacyItemService
                    .findItemById(booking.getItemId())
                    .map(Item::getPricePerHour)
                    .orElse(null);

            final User guest =
                    legacyItemService.findUserById(booking.getGuestId()).orElse(null);
            req.put("requesterName", guest != null ? guest.getName() : "");
            req.put("requesterEmail", guest != null ? guest.getEmail() : "");

            req.put("dateLabel", BookingDisplayFormatter.formatDateLabel(booking.getStartTime()));
            req.put(
                    "timeRangeLabel",
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()));
            req.put(
                    "totalPriceLabel",
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            req.put("statusMessageCode", BookingDisplayFormatter.statusMessageCode(booking.getState()));

            req.put(
                    "hasRequestMessage",
                    booking.getRequestMessage() != null
                            && !booking.getRequestMessage().isEmpty());
            req.put("requestMessage", booking.getRequestMessage() != null ? booking.getRequestMessage() : "");

            req.put("paymentAlias", null);
            req.put("hasPaymentGuestReply", false);
            req.put("paymentGuestReply", "");
            req.put("hasPaymentProof", false);
            req.put("paymentProofPdf", false);
            req.put("hasPaymentRefusalReason", false);
            req.put("paymentRefusalReason", "");
            req.put("requesterHasReviews", false);
            req.put("requesterAverageRating", 0.0);
            req.put("requesterTotalReviews", 0);
            receivedBookingRequests.add(req);
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("user", currentUser);
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("publicationDeleteDeactivatesByItemId", publicationDeleteDeactivatesByItemId);
        mav.addObject("publicationDeleteDisabledByItemId", publicationDeleteDisabledByItemId);
        mav.addObject("itemPage", new PageModel<>(ownedItems, safePage, safePageSize, totalItems));
        mav.addObject("receivedBookingRequests", receivedBookingRequests);
        mav.addObject(
                "receivedBookingPage",
                new PageModel<>(receivedBookingRequests, safePage, RECEIVED_PAGE_SIZE, totalReceived));
        mav.addObject("selectedBookingStatusFilters", List.of());
        mav.addObject("selectedBookingStatusFiltersByValue", Map.of());
        mav.addObject("boatSearchQuery", "");
        mav.addObject("receivedCanReviewByBookingId", Map.of());
        return mav;
    }

    private UserModel currentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
