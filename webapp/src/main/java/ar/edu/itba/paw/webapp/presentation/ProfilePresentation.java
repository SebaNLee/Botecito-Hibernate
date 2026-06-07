package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.paging.ReviewPaging;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileListingsViewForm;
import ar.edu.itba.paw.webapp.form.ProfileReviewsViewForm;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private static final String MESSAGE_PREFIX = "profile";
    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ToastPresentation toastPresentation;

    public ModelAndView profileListings(
            final Users profileUser,
            final PageModel<Item> listingsPage,
            final int followersCount,
            final boolean isSelf,
            final boolean isSubscribed,
            final ProfileListingsViewForm profileListingsView) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", "listings");
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", isSubscribed);
        mav.addObject("followersCount", followersCount);
        mav.addObject("listingsPage", listingsPage);
        mav.addObject("profileListingsView", profileListingsView);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView profileReviews(
            final Users profileUser,
            final PageModel<Review> reviewsPage,
            final Double averageRating,
            final int followersCount,
            final boolean isSelf,
            final boolean isSubscribed,
            final ProfileReviewsViewForm profileReviewsView) {
        final Map<Integer, String> reviewDatesById = formatReviewDates(reviewsPage);

        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", "reviews");
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", isSubscribed);
        mav.addObject("followersCount", followersCount);
        mav.addObject("reviewsPage", reviewsPage);
        mav.addObject("reviewDatesById", reviewDatesById);
        mav.addObject("averageRating", averageRating);
        mav.addObject("profileReviewsView", profileReviewsView);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView profileListingsErrors(
            final BotecitoUserDetails viewer,
            final Users profileUser,
            final ProfileListingsViewForm profileListingsView,
            final BindingResult errors) {
        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();

        final ModelAndView mav = new ModelAndView("profile");
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", "listings");
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", false);
        mav.addObject("followersCount", 0);
        mav.addObject("listingsPage", new PageModel<>(List.of(), 1, 12, 0L));
        mav.addObject("profileListingsView", profileListingsView);
        mav.addObject("hasValidationErrors", true);
        return mav;
    }

    public ModelAndView profileReviewsErrors(
            final BotecitoUserDetails viewer,
            final Users profileUser,
            final ProfileReviewsViewForm profileReviewsView,
            final BindingResult errors) {
        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();

        final ModelAndView mav = new ModelAndView("profile");
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", "reviews");
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", false);
        mav.addObject("followersCount", 0);
        mav.addObject("reviewsPage", new PageModel<>(List.of(), 1, ReviewPaging.DEFAULT_PAGE_SIZE, 0L));
        mav.addObject("reviewDatesById", Map.of());
        mav.addObject("averageRating", null);
        mav.addObject("profileReviewsView", profileReviewsView);
        mav.addObject("hasValidationErrors", true);
        return mav;
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
