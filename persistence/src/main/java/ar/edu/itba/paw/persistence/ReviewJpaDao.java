package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
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
        final Review orm = new Review();
        orm.setBooking(entityManager.getReference(Booking.class, bookingId));
        orm.setSender(entityManager.getReference(Users.class, senderUserId));
        orm.setTargetType(targetType);
        orm.setRating(BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP));
        orm.setComment(reviewComment);
        orm.setCreatedAt(LocalDateTime.now());
        entityManager.persist(orm);
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

    @Override
    public List<Review> findReviewsBySender(final int senderUserId) {
        return entityManager
                .createQuery("SELECT r FROM Review r JOIN FETCH r.booking WHERE r.sender.id = :senderId", Review.class)
                .setParameter("senderId", senderUserId)
                .getResultList();
    }

    @Override
    public List<Review> findReviewsAboutHost(final int hostUserId, final int page, final int pageSize) {
        final TypedQuery<Review> q = entityManager.createQuery(
                "SELECT r FROM Review r "
                        + "JOIN FETCH r.booking b "
                        + "JOIN FETCH r.sender s "
                        + "WHERE r.targetType = :target "
                        + "  AND b.version.item.host.id = :hostId "
                        + "  AND s.id <> :hostId "
                        + "ORDER BY r.createdAt DESC, r.id DESC",
                Review.class);
        q.setParameter("target", TargetEnum.USER);
        q.setParameter("hostId", hostUserId);
        Paging.apply(q, page, pageSize);
        return q.getResultList();
    }

    @Override
    public int countReviewsAboutHost(final int hostUserId) {
        return ((Number) entityManager
                        .createQuery("SELECT COUNT(r) FROM Review r "
                                + "WHERE r.targetType = :target "
                                + "  AND r.booking.version.item.host.id = :hostId "
                                + "  AND r.sender.id <> :hostId")
                        .setParameter("target", TargetEnum.USER)
                        .setParameter("hostId", hostUserId)
                        .getSingleResult())
                .intValue();
    }

    @Override
    public Optional<Double> averageRatingAboutHost(final int hostUserId) {
        final Object raw = entityManager
                .createQuery("SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.targetType = :target "
                        + "  AND r.booking.version.item.host.id = :hostId "
                        + "  AND r.sender.id <> :hostId")
                .setParameter("target", TargetEnum.USER)
                .setParameter("hostId", hostUserId)
                .getSingleResult();
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(((Number) raw).doubleValue());
    }
}
