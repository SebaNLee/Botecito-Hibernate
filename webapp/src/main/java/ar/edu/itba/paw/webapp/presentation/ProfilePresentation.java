package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileViewForm;
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

    public ModelAndView profile(
            final BotecitoUserDetails viewer,
            final Users profileUser,
            final String activeTab,
            final PageModel<Item> listingsPageModel,
            final List<Item> listings,
            final PageModel<Review> reviewsPageModel,
            final Double averageRating,
            final int followersCount,
            final boolean isSelf,
            final boolean isSubscribed,
            final ProfileViewForm profileView) {
        final Map<Integer, String> reviewDatesById = formatReviewDates(reviewsPageModel);

        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", activeTab);
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", isSubscribed);
        mav.addObject("followersCount", followersCount);
        mav.addObject("listings", listings);
        mav.addObject("listingsPage", listingsPageModel);
        mav.addObject("listingsTotal", listingsPageModel.getTotalItems());
        mav.addObject("reviews", reviewsPageModel.getContent());
        mav.addObject("reviewsPage", reviewsPageModel);
        mav.addObject("reviewsTotal", reviewsPageModel.getTotalItems());
        mav.addObject("reviewDatesById", reviewDatesById);
        mav.addObject("averageRating", averageRating);
        mav.addObject("profileView", profileView);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView profileErrors(
            final BotecitoUserDetails viewer,
            final Users profileUser,
            final ProfileViewForm profileView,
            final BindingResult errors) {
        final String activeTab = profileView.getTab() != null && "reviews".equalsIgnoreCase(profileView.getTab())
                ? "reviews"
                : "listings";
        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();

        final ModelAndView mav = new ModelAndView("profile");
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("user", profileUser);
        mav.addObject("activeTab", activeTab);
        mav.addObject("isSelf", isSelf);
        mav.addObject("isSubscribed", false);
        mav.addObject("followersCount", 0);
        mav.addObject("listings", List.of());
        mav.addObject("listingsPage", new PageModel<>(List.of(), 1, 12, 0));
        mav.addObject("listingsTotal", 0);
        mav.addObject("reviews", List.of());
        mav.addObject("reviewsPage", new PageModel<>(List.of(), 1, 5, 0));
        mav.addObject("reviewsTotal", 0);
        mav.addObject("reviewDatesById", Map.of());
        mav.addObject("averageRating", null);
        mav.addObject("profileView", profileView);
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
