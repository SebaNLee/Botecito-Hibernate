package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import javax.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(final ReviewService reviewService, final UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @RequestMapping(value = "/reviews/booking/{bookingId:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView createReview(
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("reviewForm") final ReviewForm form,
            final BindingResult errors,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return new ModelAndView("redirect:" + buildReturnUrl(returnTo, itemId, "validationError"));
        }

        final boolean created = reviewService
                .createReviewForBooking(bookingId, currentUser.getId(), form.getRating(), form.getComment())
                .isPresent();
        return new ModelAndView("redirect:" + buildReturnUrl(returnTo, itemId, created ? "created" : "error"));
    }

    @RequestMapping(value = "/reviews/{reviewId:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView deleteReview(
            @PathVariable("reviewId") final int reviewId,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final boolean deleted = reviewService.deleteReview(reviewId, currentUser.getId());
        return new ModelAndView("redirect:" + buildReturnUrl(returnTo, itemId, deleted ? "deleted" : "deleteError"));
    }

    private static String buildReturnUrl(final String returnTo, final Integer itemId, final String action) {
        if ("item".equals(returnTo) && itemId != null) {
            return "/item/" + itemId + "?reviewAction=" + action;
        }
        return "/profile/dashboard?dashboardTab=reviews&reviewAction=" + action + "#reviews";
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
