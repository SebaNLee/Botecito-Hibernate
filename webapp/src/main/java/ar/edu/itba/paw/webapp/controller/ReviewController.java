package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.ReviewMvcSupport;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// @Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewMvcSupport reviewMvcSupport;

    // @RequestMapping(value = "/reviews/booking/{bookingId:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView createReview(
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("reviewForm") final ReviewForm form,
            final BindingResult errors,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        return reviewMvcSupport.createReview(bookingId, form, errors, returnTo, itemId, redirectAttributes);
    }

    // @RequestMapping(value = "/reviews/{reviewId:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView deleteReview(
            @PathVariable("reviewId") final int reviewId,
            @RequestParam(value = "returnTo", required = false, defaultValue = "dashboard") final String returnTo,
            @RequestParam(value = "itemId", required = false) final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        return reviewMvcSupport.deleteReview(reviewId, returnTo, itemId, redirectAttributes);
    }
}
