package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Review;
import java.util.Optional;

public interface ReviewService {

    Optional<Review> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);
}
