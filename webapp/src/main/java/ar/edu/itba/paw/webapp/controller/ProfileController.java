package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.exceptions.UserNotFoundException;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ProfileListingsViewForm;
import ar.edu.itba.paw.webapp.form.ProfileReviewsViewForm;
import ar.edu.itba.paw.webapp.presentation.ProfilePresentation;
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

    private final UserService userService;
    private final ItemService itemService;
    private final ReviewService reviewService;
    private final SubscriptionService subscriptionService;
    private final ProfilePresentation profilePresentation;

    @ModelAttribute("profileListingsView")
    public ProfileListingsViewForm defaultProfileListingsView() {
        final ProfileListingsViewForm form = new ProfileListingsViewForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @ModelAttribute("profileReviewsView")
    public ProfileReviewsViewForm defaultProfileReviewsView() {
        final ProfileReviewsViewForm form = new ProfileReviewsViewForm();
        form.setPage(1);
        return form;
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public ModelAndView profileRedirect(@PathVariable("id") final int id) {
        return new ModelAndView("redirect:/profiles/" + id + "/listings");
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/listings", method = RequestMethod.GET)
    public ModelAndView profileListings(
            @AuthenticationPrincipal final BotecitoUserDetails viewer,
            @PathVariable("id") final int id,
            @Valid @ModelAttribute("profileListingsView") final ProfileListingsViewForm profileListingsView,
            final BindingResult errors) {
        final Users profileUser = userService.findById(id).orElseThrow(UserNotFoundException::new);

        if (errors.hasErrors()) {
            return profilePresentation.profileListingsErrors(viewer, profileUser, profileListingsView, errors);
        }

        final var listingsPage = itemService.listOwnerItems(
                id,
                null,
                null,
                profileListingsView.getPage(),
                profileListingsView.getPageSize(),
                profileListingsView.getSortBy());

        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();
        final boolean isSubscribed =
                !isSelf && viewer != null && subscriptionService.isSubscribed(viewer.getId(), profileUser.getId());
        final int followersCount = subscriptionService.countFollowers(id);

        return profilePresentation.profileListings(
                profileUser, listingsPage, followersCount, isSelf, isSubscribed, profileListingsView);
    }

    @RequestMapping(value = "/profiles/{id:[1-9]\\d*}/reviews", method = RequestMethod.GET)
    public ModelAndView profileReviews(
            @AuthenticationPrincipal final BotecitoUserDetails viewer,
            @PathVariable("id") final int id,
            @Valid @ModelAttribute("profileReviewsView") final ProfileReviewsViewForm profileReviewsView,
            final BindingResult errors) {
        final Users profileUser = userService.findById(id).orElseThrow(UserNotFoundException::new);

        if (errors.hasErrors()) {
            return profilePresentation.profileReviewsErrors(viewer, profileUser, profileReviewsView, errors);
        }

        final var hostReviewsPage = reviewService.findHostReviewsPage(id, profileReviewsView.getPage());

        final boolean isSelf = viewer != null && viewer.getId() == profileUser.getId();
        final boolean isSubscribed =
                !isSelf && viewer != null && subscriptionService.isSubscribed(viewer.getId(), profileUser.getId());
        final int followersCount = subscriptionService.countFollowers(id);

        return profilePresentation.profileReviews(
                profileUser,
                hostReviewsPage.getReviews(),
                hostReviewsPage.getAverageRating(),
                followersCount,
                isSelf,
                isSubscribed,
                profileReviewsView);
    }
}
