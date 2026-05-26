package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.UserNotFoundException;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private static final int LISTINGS_TOTAL_CAP = Integer.MAX_VALUE;

    private final UserService userService;
    private final ItemService itemService;
    private final ReviewService reviewService;
    private final SubscriptionService subscriptionService;
    private final CoverImageUrlResolver coverImageUrlResolver;

    public ModelAndView profile(
            final BotecitoUserDetails viewer,
            final int id,
            final String tab,
            final int listingsPage,
            final int listingsPageSize,
            final int reviewsPage,
            final int reviewsPageSize,
            final HttpServletRequest request) {
        final Users profileUser = userService.findById(id).orElseThrow(UserNotFoundException::new);

        final String activeTab = "reviews".equalsIgnoreCase(tab) ? "reviews" : "listings";

        final int safeListingsPage = Math.max(1, listingsPage);
        final int safeListingsPageSize = Math.max(1, listingsPageSize);
        final ItemSearchResult listingsResult = itemService.listOwnerItems(id, safeListingsPage, safeListingsPageSize);
        final long total = listingsResult.getTotalCount();
        final int totalListings = total > LISTINGS_TOTAL_CAP ? LISTINGS_TOTAL_CAP : (int) total;
        final PageModel<?> listingsPageModel =
                new PageModel<>(listingsResult.getItems(), safeListingsPage, safeListingsPageSize, totalListings);

        final PageModel<Review> reviewsPageModel = reviewService.findReviewsAboutHost(id, reviewsPage, reviewsPageSize);
        final Double averageRating = reviewService.averageRatingAboutHost(id).orElse(null);
        final int followersCount = subscriptionService.countFollowers(id);

        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();
        final boolean isSubscribed =
                !isSelf && viewer != null && subscriptionService.isSubscribed(viewer.getId(), profileUser.getId());

        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", activeTab);
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", isSubscribed);
        mav.addObject("followersCount", followersCount);
        mav.addObject("listings", listingsResult.getItems());
        mav.addObject("listingsPage", listingsPageModel);
        mav.addObject("listingsTotal", totalListings);
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(listingsResult.getItems(), request));
        mav.addObject("reviews", reviewsPageModel.getContent());
        mav.addObject("reviewsPage", reviewsPageModel);
        mav.addObject("reviewsTotal", reviewsPageModel.getTotalItems());
        mav.addObject("averageRating", averageRating);
        return mav;
    }
}
