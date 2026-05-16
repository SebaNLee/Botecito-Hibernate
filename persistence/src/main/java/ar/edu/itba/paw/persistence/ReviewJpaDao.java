package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewJpaDao implements ReviewDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Review> createReview(
            final int bookingId,
            final int senderUserId,
            final TargetEnum targetType,
            final double rating,
            final String reviewComment) {
        final Booking booking = entityManager.find(Booking.class, bookingId);
        if (booking == null) {
            return Optional.empty();
        }
        final Users sender = entityManager.find(Users.class, senderUserId);
        if (sender == null) {
            return Optional.empty();
        }
        final Review orm = new Review();
        orm.setBooking(booking);
        orm.setSender(sender);
        orm.setTargetType(targetType);
        orm.setRating(BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP));
        orm.setComment(reviewComment);
        orm.setCreatedAt(LocalDateTime.now());
        entityManager.persist(orm);
        entityManager.flush();
        return Optional.of(orm);
    }

    @Override
    public Optional<Review> findReviewByBookingSenderAndTargetType(
            final int bookingId, final int senderUserId, final TargetEnum targetType) {
        final TypedQuery<Review> q = entityManager.createQuery(
                "SELECT r FROM Review r WHERE r.booking.id = :bookingId "
                        + "AND r.sender.id = :senderId AND r.targetType = :targetType",
                Review.class);
        q.setParameter("bookingId", bookingId);
        q.setParameter("senderId", senderUserId);
        q.setParameter("targetType", targetType);
        return q.getResultStream().findFirst();
    }
}
