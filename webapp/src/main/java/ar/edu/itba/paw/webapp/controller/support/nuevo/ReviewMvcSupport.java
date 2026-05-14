package ar.edu.itba.paw.webapp.controller.support.nuevo;

import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.ReviewInterface;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.nuevo.ReviewForm;
import ar.edu.itba.paw.webapp.presentation.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class ReviewMvcSupport {

    private final ReviewInterface reviewInterface;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView createReview(
            final int bookingId,
            final ReviewForm form,
            final BindingResult errors,
            final String returnTo,
            final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final UserModel currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.reviews.validationError");
            return reviewRedirect(returnTo, itemId);
        }

        final boolean created = reviewInterface
                .createReviewForBooking(bookingId, currentUser.getId(), form.getRating(), form.getComment())
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
