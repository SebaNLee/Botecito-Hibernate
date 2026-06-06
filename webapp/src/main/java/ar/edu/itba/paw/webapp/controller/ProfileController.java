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
import ar.edu.itba.paw.webapp.form.ProfileViewForm;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private static final int LISTINGS_TOTAL_CAP = Integer.MAX_VALUE;
    private static final int REVIEWS_PAGE_SIZE = 5;

    private final UserService userService;
    private final ItemService itemService;
    private final ReviewService reviewService;
    private final SubscriptionService subscriptionService;
    private final ProfilePresentation profilePresentation;

    @ModelAttribute("profileView")
    public ProfileViewForm defaultProfileView() {
        final ProfileViewForm form = new ProfileViewForm();
        form.setTab("listings");
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView profile(
            @AuthenticationPrincipal final BotecitoUserDetails viewer,
            @PathVariable("id") final int id,
            @Valid @ModelAttribute("profileView") final ProfileViewForm profileView,
            final BindingResult errors,
            final HttpServletRequest request) {
        final Users profileUser = userService.findById(id).orElseThrow(UserNotFoundException::new);

        if (errors.hasErrors()) {
            return profilePresentation.profileErrors(viewer, profileUser, profileView, errors, request);
        }

        final String activeTab = resolveActiveTab(profileView.getTab());
        final int listingsPage = "reviews".equals(activeTab) ? 1 : profileView.getPage();
        final int reviewsPage = "reviews".equals(activeTab) ? profileView.getPage() : 1;

        final String sortBy = resolveSortBy(profileView.getSortBy());
        final SearchResult<Item> listingsResult =
                itemService.listOwnerItems(id, null, null, listingsPage, profileView.getPageSize(), sortBy);
        final long total = listingsResult.getTotalCount();
        final int totalListings = total > LISTINGS_TOTAL_CAP ? LISTINGS_TOTAL_CAP : (int) total;
        final PageModel<Item> listingsPageModel = new PageModel<>(
                listingsResult.getPageElements(), listingsPage, profileView.getPageSize(), totalListings);

        final PageModel<Review> reviewsPageModel =
                reviewService.findReviewsAboutHost(id, reviewsPage, REVIEWS_PAGE_SIZE);
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
                profileView,
                request);
    }

    private static String resolveActiveTab(final String tab) {
        return "reviews".equalsIgnoreCase(tab) ? "reviews" : "listings";
    }

    private static String resolveSortBy(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return sortBy;
    }
}
