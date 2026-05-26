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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private static final int LISTINGS_TOTAL_CAP = Integer.MAX_VALUE;
    private static final int MAX_LISTINGS_PAGE_SIZE = 24;
    private static final int MAX_REVIEWS_PAGE_SIZE = 50;
    private static final int MAX_PAGE_NUMBER = 100_000;
    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

        final int safeListingsPage = clampPage(listingsPage);
        final int safeListingsPageSize = clampPageSize(listingsPageSize, MAX_LISTINGS_PAGE_SIZE);
        final int safeReviewsPage = clampPage(reviewsPage);
        final int safeReviewsPageSize = clampPageSize(reviewsPageSize, MAX_REVIEWS_PAGE_SIZE);

        final ItemSearchResult listingsResult = itemService.listOwnerItems(id, safeListingsPage, safeListingsPageSize);
        final long total = listingsResult.getTotalCount();
        final int totalListings = total > LISTINGS_TOTAL_CAP ? LISTINGS_TOTAL_CAP : (int) total;
        final PageModel<?> listingsPageModel =
                new PageModel<>(listingsResult.getItems(), safeListingsPage, safeListingsPageSize, totalListings);

        final PageModel<Review> reviewsPageModel =
                reviewService.findReviewsAboutHost(id, safeReviewsPage, safeReviewsPageSize);
        final Double averageRating = reviewService.averageRatingAboutHost(id).orElse(null);
        final int followersCount = subscriptionService.countFollowers(id);

        final Map<Integer, String> reviewDatesById = formatReviewDates(reviewsPageModel);

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
        mav.addObject("reviewDatesById", reviewDatesById);
        mav.addObject("averageRating", averageRating);
        return mav;
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

    private static Map<Integer, String> formatReviewDates(final PageModel<Review> page) {
        final Map<Integer, String> dates = new LinkedHashMap<>();
        for (final Review review : page.getContent()) {
            if (review == null || review.getId() == null || review.getCreatedAt() == null) {
                continue;
            }
            dates.put(review.getId(), review.getCreatedAt().format(REVIEW_DATE_FORMAT));
        }
        return dates;
    }
}
