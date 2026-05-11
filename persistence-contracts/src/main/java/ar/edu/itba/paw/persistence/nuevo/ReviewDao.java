package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import java.util.List;
import java.util.Optional;

public interface ReviewDao {

    Optional<ReviewModel> createReview(
            int bookingId, int senderUserId, TargetType targetType, double rating, String reviewComment);

    Optional<ReviewModel> findReviewByBookingSenderAndTargetType(
            int bookingId, int senderUserId, TargetType targetType);

    List<ReviewModel> listLatestReviewsForItem(int itemId, int limit);

    List<ReviewModel> listReviewsBySender(int senderUserId);

    List<ReviewModel> listReviewsForUser(int userId);

    Optional<ReviewModel> findReviewById(int reviewId);

    boolean deleteReview(int reviewId, int senderUserId);

    RatingSummaryModel ratingSummaryForItem(int itemId);
}
