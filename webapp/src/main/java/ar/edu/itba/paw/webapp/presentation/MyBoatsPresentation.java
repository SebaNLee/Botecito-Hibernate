package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.models.nuevo.PageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.ItemInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
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

    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private final UserService userService;
    private final ItemInterface itemInterface;

    public ModelAndView myBoats(final HttpServletRequest request) {
        final UserModel currentUser = currentUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final List<MyBoatsItem> ownedItems = itemInterface.listMyBoatsItemsByOwnerId(currentUser.getId());
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

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("user", currentUser);
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", publicationCoverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", imageUrlsByItemId);
        mav.addObject("publicationDeleteDeactivatesByItemId", publicationDeleteDeactivatesByItemId);
        mav.addObject("publicationDeleteDisabledByItemId", publicationDeleteDisabledByItemId);
        mav.addObject("receivedBookingRequests", List.of());
        mav.addObject("receivedBookingPage", new PageModel<>(List.of(), 1, 6, 0));
        mav.addObject("selectedBookingStatusFilters", List.of());
        mav.addObject("selectedBookingStatusFiltersByValue", emptyStatusSelection());
        mav.addObject("boatSearchQuery", "");
        mav.addObject("pendingOwnerUserReviewsByBookingId", Map.of());
        mav.addObject("authoredUserReviewsByBookingId", Map.of());
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

    private static Map<String, Boolean> emptyStatusSelection() {
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
            selectedByStatus.put(status, false);
        }
        return selectedByStatus;
    }
}
