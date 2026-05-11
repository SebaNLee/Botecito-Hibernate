package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.nuevo.ReviewForm;
import ar.edu.itba.paw.webapp.presentation.AuthenticatedUserResolver;
import ar.edu.itba.paw.webapp.presentation.ReviewPresentation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final ReviewPresentation reviewPresentation;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @RequestMapping(value = "/reviews/booking/{bookingId:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView createReview(
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("reviewForm") final ReviewForm form,
            final BindingResult errors,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.reviews.validationError");
            return ReviewPresentation.reviewRedirect(returnTo, itemId);
        }
        return reviewPresentation.createReview(
                bookingId,
                currentUser.getId(),
                form.getRating(),
                form.getComment(),
                returnTo,
                itemId,
                redirectAttributes);
    }

    @RequestMapping(value = "/reviews/{reviewId:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView deleteReview(
            @PathVariable("reviewId") final int reviewId,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        return reviewPresentation.deleteReview(reviewId, currentUser.getId(), returnTo, itemId, redirectAttributes);
    }
}
