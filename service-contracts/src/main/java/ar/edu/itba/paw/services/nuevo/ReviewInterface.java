package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ReviewModel;
import java.util.Optional;

public interface ReviewInterface {

    Optional<ReviewModel> createReviewForBooking(int bookingId, int reviewerUserId, int rating, String comment);

    boolean canCreateReview(int bookingId, int reviewerUserId);
}
