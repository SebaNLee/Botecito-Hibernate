package ar.edu.itba.paw.webapp.controller.support.nuevo;

import ar.edu.itba.paw.models.nuevo.PendingReviewActionModel;
import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import ar.edu.itba.paw.services.nuevo.ReviewInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
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

    private final ReviewInterface reviewInterface;
    private final UserService userService;

    public void addMarketplaceItemReviewData(final ModelAndView mav, final int itemId, final Integer viewerUserId) {
        final RatingSummaryModel ratingSummary = reviewInterface.getItemRatingSummary(itemId);
        final List<ReviewModel> reviews = reviewInterface.listLatestItemReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final ReviewModel review : reviews) {
            final int senderId = review.getSenderId();
            if (senderId <= 0 || reviewAuthorNames.containsKey(senderId)) {
                continue;
            }
            reviewAuthorNames.put(
                    senderId,
                    userService.findById(senderId).map(UserModel::getName).orElse(""));
        }
        final Map<Integer, String> reviewCreatedAtLabels = buildReviewCreatedAtLabels(reviews);
        final PendingReviewActionModel pendingItemReviewAction = viewerUserId == null
                ? null
                : reviewInterface
                        .findPendingItemReviewAction(viewerUserId, itemId)
                        .orElse(null);

        mav.addObject("itemRatingSummary", ratingSummary);
        mav.addObject("itemReviews", reviews);
        mav.addObject("reviewAuthorNames", reviewAuthorNames);
        mav.addObject("reviewCreatedAtLabels", reviewCreatedAtLabels);
        mav.addObject("pendingItemReviewAction", pendingItemReviewAction);
    }

    public void addDashboardReviewData(final ModelAndView mav, final int userId) {
        final Map<Integer, PendingReviewActionModel> pendingItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, PendingReviewActionModel> pendingUserReviewsByBookingId = new LinkedHashMap<>();
        for (final PendingReviewActionModel action : reviewInterface.listPendingReviewActions(userId)) {
            if (action.getTargetType() == TargetType.ITEM) {
                pendingItemReviewsByBookingId.put(action.getBookingId(), action);
            } else if (action.getTargetType() == TargetType.USER) {
                pendingUserReviewsByBookingId.put(action.getBookingId(), action);
            }
        }

        final Map<Integer, ReviewModel> authoredItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, ReviewModel> authoredUserReviewsByBookingId = new LinkedHashMap<>();
        for (final ReviewModel review : reviewInterface.listAuthoredReviews(userId)) {
            if (review.getBookingId() <= 0) {
                continue;
            }
            if (review.getTargetType() == TargetType.ITEM) {
                authoredItemReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            } else if (review.getTargetType() == TargetType.USER) {
                authoredUserReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            }
        }

        mav.addObject("pendingGuestItemReviewsByBookingId", pendingItemReviewsByBookingId);
        mav.addObject("pendingOwnerUserReviewsByBookingId", pendingUserReviewsByBookingId);
        mav.addObject("authoredItemReviewsByBookingId", authoredItemReviewsByBookingId);
        mav.addObject("authoredUserReviewsByBookingId", authoredUserReviewsByBookingId);
    }

    private static Map<Integer, String> buildReviewCreatedAtLabels(final List<ReviewModel> reviews) {
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
}
