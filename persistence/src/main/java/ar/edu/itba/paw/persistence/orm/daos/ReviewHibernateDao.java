package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingOrm;
import ar.edu.itba.paw.persistence.orm.entities.ReviewOrm;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ReviewHibernateDao implements ReviewDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ReviewModel> createReview(
            final int bookingId,
            final int senderUserId,
            final TargetType targetType,
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
        orm.setTargetType(TargetEnumOrm.valueOf(targetType.name()));
        orm.setRating(BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP));
        orm.setComment(reviewComment);
        orm.setCreatedAt(LocalDateTime.now());
        entityManager.persist(orm);
        entityManager.flush();
        return Optional.of(toModel(orm));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewModel> findReviewByBookingSenderAndTargetType(
            final int bookingId, final int senderUserId, final TargetType targetType) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(
                "SELECT r FROM ReviewOrm r WHERE r.booking.id = :bookingId "
                        + "AND r.sender.id = :senderId AND r.targetType = :targetType",
                ReviewOrm.class);
        q.setParameter("bookingId", bookingId);
        q.setParameter("senderId", senderUserId);
        q.setParameter("targetType", TargetEnumOrm.valueOf(targetType.name()));
        return q.getResultStream().findFirst().map(ReviewHibernateDao::toModel);
    }

    private static ReviewModel toModel(final ReviewOrm orm) {
        final ReviewModel m = new ReviewModel();
        m.setId(orm.getId() == null ? 0 : orm.getId());
        m.setBookingId(
                orm.getBooking() == null || orm.getBooking().getId() == null
                        ? 0
                        : orm.getBooking().getId());
        m.setSenderId(
                orm.getSender() == null || orm.getSender().getId() == null
                        ? 0
                        : orm.getSender().getId());
        m.setTargetType(TargetType.valueOf(orm.getTargetType().name()));
        m.setRating(orm.getRating() == null ? 0.0 : orm.getRating().doubleValue());
        m.setComment(orm.getComment());
        m.setCreatedAt(orm.getCreatedAt());
        return m;
    }
}
