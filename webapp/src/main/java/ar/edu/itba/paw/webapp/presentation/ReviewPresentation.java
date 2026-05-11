package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.DashboardReviewView;
import ar.edu.itba.paw.models.nuevo.ItemReviewsView;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.services.nuevo.ReviewInterface;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class ReviewPresentation {

    private final ReviewInterface reviewInterface;

    public ModelAndView createReview(
            final int bookingId,
            final int currentUserId,
            final int rating,
            final String comment,
            final String returnTo,
            final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final boolean created = reviewInterface
                .createReviewForBooking(bookingId, currentUserId, rating, comment)
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
            final int currentUserId,
            final String returnTo,
            final Integer itemId,
            final RedirectAttributes redirectAttributes) {
        final boolean deleted = reviewInterface.deleteReview(reviewId, currentUserId);
        if (deleted) {
            ToastSupport.success(redirectAttributes, "profile.reviews.deleted");
        } else {
            ToastSupport.error(redirectAttributes, "profile.reviews.error");
        }
        return reviewRedirect(returnTo, itemId);
    }

    public static ModelAndView reviewRedirect(final String returnTo, final Integer itemId) {
        if ("item".equals(returnTo) && itemId != null) {
            return new ModelAndView("redirect:/item/" + itemId);
        }
        if ("dashboardHosting".equals(returnTo)) {
            return new ModelAndView("redirect:/my-boats#received-booking-requests");
        }
        return new ModelAndView("redirect:/bookings#sent-booking-requests");
    }

    public void addMarketplaceItemReviewData(final ModelAndView mav, final int itemId, final Integer viewerUserId) {
        final ItemReviewsView view = reviewInterface.getItemReviewsView(itemId, 12, viewerUserId);
        mav.addObject("itemRatingSummary", view.getRatingSummary());
        mav.addObject("itemReviews", view.getReviews());
        mav.addObject("reviewAuthorNames", view.getAuthorNamesBySenderId());
        mav.addObject("reviewCreatedAtLabels", buildReviewCreatedAtLabels(view.getReviews()));
        mav.addObject("pendingItemReviewAction", view.getPendingViewerAction());
    }

    public void addDashboardReviewData(final ModelAndView mav, final int userId) {
        final DashboardReviewView view = reviewInterface.getDashboardReviewView(userId);
        mav.addObject("pendingGuestItemReviewsByBookingId", view.getPendingItemReviewsByBookingId());
        mav.addObject("pendingOwnerUserReviewsByBookingId", view.getPendingUserReviewsByBookingId());
        mav.addObject("authoredItemReviewsByBookingId", view.getAuthoredItemReviewsByBookingId());
        mav.addObject("authoredUserReviewsByBookingId", view.getAuthoredUserReviewsByBookingId());
    }

    public Map<Integer, String> buildReviewCreatedAtLabels(final List<ReviewModel> reviews) {
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        final Map<Integer, String> labels = new LinkedHashMap<>();
        for (final ReviewModel review : reviews) {
            if (review.getCreatedAt() == null) {
                continue;
            }
            labels.put(review.getId(), formatter.format(review.getCreatedAt()));
        }
        return labels;
    }

    public Map<Integer, Integer> buildReviewFullStars(final List<ReviewModel> reviews) {
        final Map<Integer, Integer> stars = new LinkedHashMap<>();
        for (final ReviewModel review : reviews) {
            stars.put(review.getId(), (int) Math.floor(review.getRating()));
        }
        return stars;
    }
}
