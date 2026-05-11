package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
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
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

    @Override
    @Transactional(readOnly = true)
    public List<ReviewModel> listLatestReviewsForItem(final int itemId, final int limit) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(
                "SELECT r FROM ReviewOrm r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender "
                        + "WHERE r.targetType = :targetType AND b.version.item.id = :itemId "
                        + "ORDER BY r.createdAt DESC",
                ReviewOrm.class);
        q.setParameter("targetType", TargetEnumOrm.ITEM);
        q.setParameter("itemId", itemId);
        if (limit > 0) {
            q.setMaxResults(limit);
        }
        return q.getResultList().stream().map(ReviewHibernateDao::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewModel> listReviewsBySender(final int senderUserId) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(
                "SELECT r FROM ReviewOrm r JOIN FETCH r.booking WHERE r.sender.id = :senderId "
                        + "ORDER BY r.createdAt DESC",
                ReviewOrm.class);
        q.setParameter("senderId", senderUserId);
        return q.getResultList().stream().map(ReviewHibernateDao::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewModel> listReviewsForUser(final int userId) {
        final TypedQuery<ReviewOrm> q = entityManager.createQuery(
                "SELECT r FROM ReviewOrm r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender "
                        + "WHERE (r.targetType = :itemTarget AND b.version.item.host.id = :userId) "
                        + "   OR (r.targetType = :userTarget "
                        + "       AND ((b.version.item.host.id = :userId AND b.guest.id <> :userId) "
                        + "         OR (b.guest.id = :userId AND b.version.item.host.id <> :userId))) "
                        + "ORDER BY r.createdAt DESC",
                ReviewOrm.class);
        q.setParameter("itemTarget", TargetEnumOrm.ITEM);
        q.setParameter("userTarget", TargetEnumOrm.USER);
        q.setParameter("userId", userId);
        return q.getResultList().stream().map(ReviewHibernateDao::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewModel> findReviewById(final int reviewId) {
        return Optional.ofNullable(entityManager.find(ReviewOrm.class, reviewId))
                .map(ReviewHibernateDao::toModel);
    }

    @Override
    public boolean deleteReview(final int reviewId, final int senderUserId) {
        final int rows = entityManager
                .createQuery("DELETE FROM ReviewOrm r WHERE r.id = :id AND r.sender.id = :senderId")
                .setParameter("id", reviewId)
                .setParameter("senderId", senderUserId)
                .executeUpdate();
        return rows > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryModel ratingSummaryForItem(final int itemId) {
        try {
            final Object[] row = (Object[]) entityManager
                    .createQuery("SELECT AVG(r.rating), COUNT(r) FROM ReviewOrm r "
                            + "WHERE r.targetType = :targetType AND r.booking.version.item.id = :itemId")
                    .setParameter("targetType", TargetEnumOrm.ITEM)
                    .setParameter("itemId", itemId)
                    .getSingleResult();
            final Number avg = (Number) row[0];
            final Number count = (Number) row[1];
            if (count == null || count.longValue() == 0L) {
                return RatingSummaryModel.empty();
            }
            return new RatingSummaryModel(avg == null ? 0.0 : avg.doubleValue(), count.longValue());
        } catch (final NoResultException e) {
            return RatingSummaryModel.empty();
        }
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
