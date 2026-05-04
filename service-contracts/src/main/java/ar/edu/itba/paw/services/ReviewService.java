package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewService {
    Optional<Review> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);

    boolean deleteReview(int reviewId, int reviewerUserId);

    List<Review> listLatestItemReviews(int itemId, int limit);

    RatingSummary getItemRatingSummary(int itemId);

    Map<Integer, RatingSummary> getItemRatingSummaries(List<Integer> itemIds);

    List<PendingReviewAction> listPendingReviewActions(int userId);

    Optional<PendingReviewAction> findPendingItemReviewAction(int userId, int itemId);

    List<Review> listAuthoredReviews(int reviewerUserId);

    List<Review> listReceivedReviews(int revieweeUserId);

    record PendingReviewAction(
            int bookingId,
            int itemId,
            int targetUserId,
            ReviewTargetType targetType,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String targetName,
            String targetEmail) {

        public int getBookingId() {
            return bookingId;
        }

        public int getItemId() {
            return itemId;
        }

        public int getTargetUserId() {
            return targetUserId;
        }

        public ReviewTargetType getTargetType() {
            return targetType;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getTargetEmail() {
            return targetEmail;
        }
    }
}
