package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.entity.ReviewOrm;
import ar.edu.itba.paw.models.entity.TargetEnumOrm;
import java.util.Optional;

public interface ReviewDao {

    Optional<ReviewOrm> createReview(
            int bookingId, int senderUserId, TargetEnumOrm targetType, double rating, String reviewComment);

    Optional<ReviewOrm> findReviewByBookingSenderAndTargetType(
            int bookingId, int senderUserId, TargetEnumOrm targetType);
}
