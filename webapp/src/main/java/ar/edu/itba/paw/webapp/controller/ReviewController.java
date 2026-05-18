package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewInterface;

    @RequestMapping(value = "/reviews/booking/{bookingId:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView createReview(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("reviewForm") final ReviewForm form,
            final BindingResult errors,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.reviews.validationError");
            return reviewRedirect(returnTo, itemId);
        }

        final boolean created = reviewInterface
                .createReviewForBooking(bookingId, user.getId(), form.getRating(), form.getComment())
                .isPresent();
        if (created) {
            ToastSupport.success(redirectAttributes, "profile.reviews.created");
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
            return new ModelAndView("redirect:/requests/incoming");
        }
        return new ModelAndView("redirect:/bookings#sent-booking-requests");
    }
}
