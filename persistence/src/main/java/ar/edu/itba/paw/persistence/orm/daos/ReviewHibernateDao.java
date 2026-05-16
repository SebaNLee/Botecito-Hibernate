package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.ReviewOrm;
import ar.edu.itba.paw.models.entity.TargetEnumOrm;
import ar.edu.itba.paw.models.entity.UsersOrm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewHibernateDao implements ReviewDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ReviewOrm> createReview(
            final int bookingId,
            final int senderUserId,
            final TargetEnumOrm targetType,
            final double rating,
            final String reviewComment) {
        final BookingOrm booking = entityManager.find(BookingOrm.class, bookingId);
        if (booking == null) {
            return Optional.empty();
        }
        final UsersOrm sender = entityManager.find(UsersOrm.class, senderUserId);
        if (sender == null) {
            return Optional.empty();
        }
        final ReviewOrm orm = new ReviewOrm();
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
    public Optional<ReviewOrm> findReviewByBookingSenderAndTargetType(
            final int bookingId, final int senderUserId, final TargetEnumOrm targetType) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(
                "SELECT r FROM ReviewOrm r WHERE r.booking.id = :bookingId "
                        + "AND r.sender.id = :senderId AND r.targetType = :targetType",
                ReviewOrm.class);
        q.setParameter("bookingId", bookingId);
        q.setParameter("senderId", senderUserId);
        q.setParameter("targetType", targetType);
        return q.getResultStream().findFirst();
    }
}
