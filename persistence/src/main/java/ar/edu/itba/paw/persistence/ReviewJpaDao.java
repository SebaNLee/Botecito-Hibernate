package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.HostReviewStats;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.utils.Paging;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewJpaDao implements ReviewDao {

    private static final String TARGET_PARAM = "target";

    private static final String ITEM_REVIEW_JOIN = "FROM review r INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:" + TARGET_PARAM + " AS target_enum) ";

    private static final String BATCH_ITEM_REVIEW_AGG_SQL =
            "SELECT v2.item_id, COUNT(r.id), COALESCE(AVG(r.rating), 0) " + ITEM_REVIEW_JOIN
                    + "AND v2.item_id IN :itemIds GROUP BY v2.item_id";

    private static final String SINGLE_ITEM_REVIEW_AGG_SQL =
            "SELECT COUNT(r.id), COALESCE(AVG(r.rating), 0) " + ITEM_REVIEW_JOIN + "AND v2.item_id = :itemId";

    private static final String SQL_REVIEW_IDS_FOR_ITEM = "SELECT r.id FROM review r "
            + "INNER JOIN booking b ON r.booking_id = b.id "
            + "INNER JOIN version v2 ON b.version_id = v2.id "
            + "WHERE r.target_type = CAST(:itemTargetType AS target_enum) AND v2.item_id = :itemId "
            + "ORDER BY r.created_at DESC";

    private static final String REVIEW_FETCH_JPQL =
            "FROM Review r JOIN FETCH r.booking b LEFT JOIN FETCH r.sender WHERE r.id IN :ids "
                    + "ORDER BY r.createdAt DESC";

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
    public HostReviewStats hostReviewStats(final int hostUserId) {
        final Object[] row = (Object[]) entityManager
                .createQuery("SELECT COUNT(r), AVG(r.rating) FROM Review r "
                        + "WHERE r.targetType = :target "
                        + "  AND r.booking.version.item.host.id = :hostId "
                        + "  AND r.sender.id <> :hostId")
                .setParameter("target", TargetEnum.USER)
                .setParameter("hostId", hostUserId)
                .getSingleResult();
        final long totalReviews = row[0] == null ? 0L : ((Number) row[0]).longValue();
        if (totalReviews == 0L) {
            return new HostReviewStats(0L, Optional.empty());
        }
        return new HostReviewStats(totalReviews, Optional.of(((Number) row[1]).doubleValue()));
    }

    @Override
    public ReviewSummary reviewSummaryForItem(final int itemId) {
        final Query query = entityManager.createNativeQuery(SINGLE_ITEM_REVIEW_AGG_SQL);
        query.setParameter(TARGET_PARAM, TargetEnum.ITEM.name());
        query.setParameter("itemId", itemId);
        final Object[] row = (Object[]) query.getSingleResult();
        return new ReviewSummary(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
    }

    @Override
    public Map<Integer, ReviewSummary> reviewSummariesForItems(final Collection<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        final Query query = entityManager.createNativeQuery(BATCH_ITEM_REVIEW_AGG_SQL);
        query.setParameter(TARGET_PARAM, TargetEnum.ITEM.name());
        query.setParameter("itemIds", itemIds);
        final Map<Integer, ReviewSummary> result = new HashMap<>();
        for (final Object rowObj : query.getResultList()) {
            final Object[] row = (Object[]) rowObj;
            result.put(
                    ((Number) row[0]).intValue(),
                    new ReviewSummary(((Number) row[1]).longValue(), ((Number) row[2]).doubleValue()));
        }
        return result;
    }

    @Override
    public List<Review> findReviewsAboutItem(final int itemId, final int page, final int pageSize) {
        final Query reviewIdsQuery = entityManager.createNativeQuery(SQL_REVIEW_IDS_FOR_ITEM);
        reviewIdsQuery.setParameter("itemId", itemId);
        reviewIdsQuery.setParameter("itemTargetType", TargetEnum.ITEM.name());
        Paging.apply(reviewIdsQuery, page, pageSize);

        final List<Integer> reviewIds = Paging.toIntegerIds(reviewIdsQuery.getResultList());
        if (reviewIds.isEmpty()) {
            return List.of();
        }

        return entityManager
                .createQuery(REVIEW_FETCH_JPQL, Review.class)
                .setParameter("ids", reviewIds)
                .getResultList();
    }
}
