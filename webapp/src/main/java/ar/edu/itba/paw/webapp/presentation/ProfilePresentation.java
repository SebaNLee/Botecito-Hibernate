package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileViewForm;
import ar.edu.itba.paw.webapp.presentation.util.CoverImageUrlResolver;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class ProfilePresentation {

    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CoverImageUrlResolver coverImageUrlResolver;

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
            final HttpServletRequest request) {
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
        mav.addObject("imageUrlsByItemId", coverImageUrlResolver.resolve(listings, request));
        mav.addObject("reviews", reviewsPageModel.getContent());
        mav.addObject("reviewsPage", reviewsPageModel);
        mav.addObject("reviewsTotal", reviewsPageModel.getTotalItems());
        mav.addObject("reviewDatesById", reviewDatesById);
        mav.addObject("averageRating", averageRating);
        mav.addObject("profileView", profileView(activeTab, listingsPageModel, reviewsPageModel));
        return mav;
    }

    private static ProfileViewForm profileView(
            final String activeTab, final PageModel<Item> listingsPageModel, final PageModel<Review> reviewsPageModel) {
        final ProfileViewForm view = new ProfileViewForm();
        view.setTab(activeTab);
        view.setListingsPage(listingsPageModel.getPage());
        view.setListingsPageSize(listingsPageModel.getPageSize());
        view.setReviewsPage(reviewsPageModel.getPage());
        view.setReviewsPageSize(reviewsPageModel.getPageSize());
        return view;
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
