package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import java.util.Optional;

public interface ReviewDao {

    Optional<ReviewModel> createReview(
            int bookingId, int senderUserId, TargetType targetType, double rating, String reviewComment);

    Optional<ReviewModel> findReviewByBookingSenderAndTargetType(
            int bookingId, int senderUserId, TargetType targetType);
}
