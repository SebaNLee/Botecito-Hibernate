package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.HostReviewStats;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewDao {

    Optional<Review> createReview(
            int bookingId, int senderUserId, TargetEnum targetType, double rating, String reviewComment);

    Optional<Review> findReviewByBookingSenderAndTargetType(int bookingId, int senderUserId, TargetEnum targetType);

    List<Review> findReviewsBySenderAndBookingIds(int senderUserId, Collection<Integer> bookingIds);

    List<Review> findReviewsAboutHost(int hostUserId, int page, int pageSize);

    HostReviewStats hostReviewStats(int hostUserId);

    ReviewSummary reviewSummaryForItem(int itemId);

    Map<Integer, ReviewSummary> reviewSummariesForItems(Collection<Integer> itemIds);

    List<Review> findReviewsAboutItem(int itemId, int page, int pageSize);
}
