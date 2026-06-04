package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.UserNotFoundException;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private static final int LISTINGS_TOTAL_CAP = Integer.MAX_VALUE;
    private static final int MAX_LISTINGS_PAGE_SIZE = 24;
    private static final int MAX_REVIEWS_PAGE_SIZE = 50;
    private static final int MAX_PAGE_NUMBER = 100_000;

    private final UserService userService;
    private final ItemService itemService;
    private final ReviewService reviewService;
    private final SubscriptionService subscriptionService;
    private final ProfilePresentation profilePresentation;

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView profile(
            @AuthenticationPrincipal final BotecitoUserDetails viewer,
            @PathVariable("id") final int id,
            @RequestParam(value = "tab", defaultValue = "listings") final String tab,
            @RequestParam(value = "listingsPage", defaultValue = "1") final int listingsPage,
            @RequestParam(value = "listingsPageSize", defaultValue = "6") final int listingsPageSize,
            @RequestParam(value = "reviewsPage", defaultValue = "1") final int reviewsPage,
            @RequestParam(value = "reviewsPageSize", defaultValue = "5") final int reviewsPageSize,
            final HttpServletRequest request) {
        final Users profileUser = userService.findById(id).orElseThrow(UserNotFoundException::new);

        final String activeTab = "reviews".equalsIgnoreCase(tab) ? "reviews" : "listings";

        final int safeListingsPage = clampPage(listingsPage);
        final int safeListingsPageSize = clampPageSize(listingsPageSize, MAX_LISTINGS_PAGE_SIZE);
        final int safeReviewsPage = clampPage(reviewsPage);
        final int safeReviewsPageSize = clampPageSize(reviewsPageSize, MAX_REVIEWS_PAGE_SIZE);

        final SearchResult<Item> listingsResult =
                itemService.listOwnerItems(id, null, null, safeListingsPage, safeListingsPageSize, "newest");
        final long total = listingsResult.getTotalCount();
        final int totalListings = total > LISTINGS_TOTAL_CAP ? LISTINGS_TOTAL_CAP : (int) total;
        final PageModel<Item> listingsPageModel = new PageModel<>(
                listingsResult.getPageElements(), safeListingsPage, safeListingsPageSize, totalListings);

        final PageModel<Review> reviewsPageModel =
                reviewService.findReviewsAboutHost(id, safeReviewsPage, safeReviewsPageSize);
        final Double averageRating = reviewService.averageRatingAboutHost(id).orElse(null);
        final int followersCount = subscriptionService.countFollowers(id);

        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();
        final boolean isSubscribed =
                !isSelf && viewer != null && subscriptionService.isSubscribed(viewer.getId(), profileUser.getId());

        return profilePresentation.profile(
                viewer,
                profileUser,
                activeTab,
                listingsPageModel,
                listingsResult.getPageElements(),
                reviewsPageModel,
                averageRating,
                followersCount,
                isSelf,
                isSubscribed,
                request);
    }

    private static int clampPage(final int page) {
        if (page < 1) {
            return 1;
        }
        return Math.min(MAX_PAGE_NUMBER, page);
    }

    private static int clampPageSize(final int pageSize, final int max) {
        if (pageSize < 1) {
            return 1;
        }
        return Math.min(max, pageSize);
    }
}
