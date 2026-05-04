package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import java.util.List;
import java.util.Optional;

public interface ReviewDao {

    Optional<Review> createReview(
            int bookingId,
            int reviewerUserId,
            int revieweeUserId,
            ReviewTargetType targetType,
            int targetId,
            int rating,
            String reviewComment);

    Optional<Review> findReviewByBookingReviewerAndTargetType(
            int bookingId, int reviewerUserId, ReviewTargetType targetType);

    List<Review> listReviewsByTarget(ReviewTargetType targetType, int targetId);

    List<Review> listLatestReviewsByTarget(ReviewTargetType targetType, int targetId, int limit);

    List<Review> listReviewsByReviewer(int reviewerUserId);

    List<Review> listReviewsByReviewee(int revieweeUserId);

    Optional<Review> findReviewById(int reviewId);

    boolean deleteReview(int reviewId, int reviewerUserId);

    RatingSummary ratingSummaryByTarget(ReviewTargetType targetType, int targetId);
}
