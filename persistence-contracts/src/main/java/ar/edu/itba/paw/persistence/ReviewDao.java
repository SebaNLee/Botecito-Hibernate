package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import java.util.List;
import java.util.Optional;

public interface ReviewDao {

    Optional<Review> createReview(
            int bookingId, int senderUserId, TargetEnum targetType, double rating, String reviewComment);

    Optional<Review> findReviewByBookingSenderAndTargetType(int bookingId, int senderUserId, TargetEnum targetType);

    List<Review> findReviewsBySender(int senderUserId);

    List<Review> findReviewsAboutHost(int hostUserId, int page, int pageSize);

    int countReviewsAboutHost(int hostUserId);

    Optional<Double> averageRatingAboutHost(int hostUserId);
}
