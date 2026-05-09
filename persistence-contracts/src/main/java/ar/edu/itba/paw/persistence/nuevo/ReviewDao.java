package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.ReviewTargetType;
import java.util.List;
import java.util.Optional;

public interface ReviewDao {

    Optional<ReviewModel> createReview(
            int bookingId,
            int reviewerUserId,
            int revieweeUserId,
            ReviewTargetType targetType,
            int targetId,
            int rating,
            String reviewComment);

    Optional<ReviewModel> findReviewByBookingReviewerAndTargetType(
            int bookingId, int reviewerUserId, ReviewTargetType targetType);

    List<ReviewModel> listReviewsByTarget(ReviewTargetType targetType, int targetId);

    List<ReviewModel> listLatestReviewsByTarget(ReviewTargetType targetType, int targetId, int limit);

    List<ReviewModel> listReviewsByReviewer(int reviewerUserId);

    List<ReviewModel> listReviewsByReviewee(int revieweeUserId);

    Optional<ReviewModel> findReviewById(int reviewId);

    boolean deleteReview(int reviewId, int reviewerUserId);

    RatingSummaryModel ratingSummaryByTarget(ReviewTargetType targetType, int targetId);
}
