package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewService {

    Optional<Review> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);

    Optional<Review> createReviewForBooking(
            int bookingId, int reviewerUserId, int rating, String comment, TargetEnum targetType);

    Map<Integer, List<Review>> findReviewsByBookingIds(int reviewerUserId);

    PageModel<Review> findReviewsAboutHost(int hostUserId, int page, int pageSize);

    Optional<Double> averageRatingAboutHost(int hostUserId);
}
