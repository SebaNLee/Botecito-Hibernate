package ar.edu.itba.paw.webapp.controller.support.nuevo;

import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
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

@Component
@RequiredArgsConstructor
public class ReviewViewHelper {

    private final ReviewService reviewService;
    private final UserService userService;

    public void addMarketplaceItemReviewData(final ModelAndView mav, final int itemId, final Integer viewerUserId) {
        final RatingSummary ratingSummary = reviewService.getItemRatingSummary(itemId);
        final List<Review> reviews = reviewService.listLatestItemReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getReviewerUserId() == null || reviewAuthorNames.containsKey(review.getReviewerUserId())) {
                continue;
            }
            reviewAuthorNames.put(
                    review.getReviewerUserId(),
                    userService
                            .findById(review.getReviewerUserId())
                            .map(User::getName)
                            .orElse(""));
        }
        final Map<Integer, String> reviewCreatedAtLabels = buildReviewCreatedAtLabels(reviews);
        final Object pendingItemReviewAction = viewerUserId == null
                ? null
                : reviewService
                        .findPendingItemReviewAction(viewerUserId, itemId)
                        .orElse(null);

        mav.addObject("itemRatingSummary", ratingSummary);
        mav.addObject("itemReviews", reviews);
        mav.addObject("reviewAuthorNames", reviewAuthorNames);
        mav.addObject("reviewCreatedAtLabels", reviewCreatedAtLabels);
        mav.addObject("pendingItemReviewAction", pendingItemReviewAction);
    }

    public void addDashboardReviewData(final ModelAndView mav, final int userId) {
        final Map<Integer, ReviewService.PendingReviewAction> pendingItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, ReviewService.PendingReviewAction> pendingUserReviewsByBookingId = new LinkedHashMap<>();
        for (final ReviewService.PendingReviewAction action : reviewService.listPendingReviewActions(userId)) {
            if (action.getTargetType() == ReviewTargetType.ITEM) {
                pendingItemReviewsByBookingId.put(action.getBookingId(), action);
            } else if (action.getTargetType() == ReviewTargetType.USER) {
                pendingUserReviewsByBookingId.put(action.getBookingId(), action);
            }
        }

        final Map<Integer, Review> authoredItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, Review> authoredUserReviewsByBookingId = new LinkedHashMap<>();
        for (final Review review : reviewService.listAuthoredReviews(userId)) {
            if (review.getBookingId() == null) {
                continue;
            }
            if (review.getTargetType() == ReviewTargetType.ITEM) {
                authoredItemReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            } else if (review.getTargetType() == ReviewTargetType.USER) {
                authoredUserReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            }
        }

        mav.addObject("pendingGuestItemReviewsByBookingId", pendingItemReviewsByBookingId);
        mav.addObject("pendingOwnerUserReviewsByBookingId", pendingUserReviewsByBookingId);
        mav.addObject("authoredItemReviewsByBookingId", authoredItemReviewsByBookingId);
        mav.addObject("authoredUserReviewsByBookingId", authoredUserReviewsByBookingId);
    }

    private static Map<Integer, String> buildReviewCreatedAtLabels(final List<Review> reviews) {
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        final Map<Integer, String> labels = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getId() == null || review.getCreatedAt() == null) {
                continue;
            }
            labels.put(review.getId(), formatter.format(review.getCreatedAt()));
        }
        return labels;
    }
}
