package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Old support helper for the legacy ReviewController (now disabled). Kept on disk for the cut-over PR; not a Spring
// bean.
@RequiredArgsConstructor
public final class ReviewMvcSupport {

    private final ReviewService reviewService;
    private final UserService userService;

    public ModelAndView createReview(
            final int bookingId,
            final ReviewForm form,
            final BindingResult errors,
            final String returnTo,
            final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.reviews.validationError");
            return reviewRedirect(returnTo, itemId);
        }

        final boolean created = reviewService
                .createReviewForBooking(bookingId, currentUser.getId(), form.getRating(), form.getComment())
                .isPresent();
        if (created) {
            ToastSupport.success(redirectAttributes, "profile.reviews.created");
        } else {
            ToastSupport.error(redirectAttributes, "profile.reviews.error");
        }
        return reviewRedirect(returnTo, itemId);
    }

    public ModelAndView deleteReview(
            final int reviewId,
            final String returnTo,
            final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final boolean deleted = reviewService.deleteReview(reviewId, currentUser.getId());
        if (deleted) {
            ToastSupport.success(redirectAttributes, "profile.reviews.deleted");
        } else {
            ToastSupport.error(redirectAttributes, "profile.reviews.error");
        }
        return reviewRedirect(returnTo, itemId);
    }

    private static ModelAndView reviewRedirect(final String returnTo, final Integer itemId) {
        if ("item".equals(returnTo) && itemId != null) {
            return new ModelAndView("redirect:/item/" + itemId);
        }
        if ("dashboardHosting".equals(returnTo)) {
            return new ModelAndView("redirect:/my-boats#received-booking-requests");
        }
        return new ModelAndView("redirect:/bookings#sent-booking-requests");
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
