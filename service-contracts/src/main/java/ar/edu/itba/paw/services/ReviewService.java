package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Review;
import java.util.Map;
import java.util.Optional;

public interface ReviewService {

    Optional<Review> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);

    Map<Integer, Review> findReviewsByBookingIds(int reviewerUserId);
}
