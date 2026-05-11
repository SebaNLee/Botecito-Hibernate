package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PendingReviewActionModel;
import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import java.util.List;
import java.util.Optional;

public interface ReviewInterface {

    Optional<ReviewModel> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);

    boolean deleteReview(int reviewId, int reviewerUserId);

    List<ReviewModel> listLatestItemReviews(int itemId, int limit);

    RatingSummaryModel getItemRatingSummary(int itemId);

    List<PendingReviewActionModel> listPendingReviewActions(int userId);

    Optional<PendingReviewActionModel> findPendingItemReviewAction(int userId, int itemId);

    List<ReviewModel> listAuthoredReviews(int reviewerUserId);

    List<ReviewModel> listReceivedReviews(int revieweeUserId);
}
