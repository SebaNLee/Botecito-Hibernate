package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.ReviewTargetType;
import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import ar.edu.itba.paw.persistence.orm.entities.BookingOrm;
import ar.edu.itba.paw.persistence.orm.entities.ReviewOrm;
import ar.edu.itba.paw.persistence.orm.entities.TargetEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import ar.edu.itba.paw.persistence.orm.projections.ReviewRowOrm;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ReviewHibernateDao implements ReviewDao {

    private static final String ITEM_TARGET_PARAM = "itemTarget";

    private static final String ROW_SELECT = "SELECT new ar.edu.itba.paw.persistence.orm.projections.ReviewRowOrm("
            + "r.id, b.id, sender.id, r.targetType, "
            + "CASE WHEN r.targetType = :" + ITEM_TARGET_PARAM + " THEN i.id ELSE b.guest.id END, "
            + "r.rating, r.comment, r.createdAt"
            + ") "
            + "FROM ReviewOrm r "
            + "JOIN r.booking b "
            + "JOIN b.version v "
            + "JOIN v.item i "
            + "LEFT JOIN r.sender sender";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Optional<ReviewModel> createReview(
            final int bookingId,
            final int reviewerUserId,
            final int revieweeUserId,
            final ReviewTargetType targetType,
            final int targetId,
            final int rating,
            final String reviewComment) {
        if (findReviewByBookingReviewerAndTargetType(bookingId, reviewerUserId, targetType)
                .isPresent()) {
            return Optional.empty();
        }

        final BookingOrm booking = entityManager.find(BookingOrm.class, bookingId);
        if (booking == null) {
            return Optional.empty();
        }
        final UsersOrm sender = entityManager.find(UsersOrm.class, reviewerUserId);
        if (sender == null) {
            return Optional.empty();
        }

        final ReviewOrm review = new ReviewOrm();
        review.setBooking(booking);
        review.setSender(sender);
        review.setTargetType(TargetEnumOrm.valueOf(targetType.name()));
        review.setRating(BigDecimal.valueOf(rating));
        review.setComment(reviewComment);
        review.setCreatedAt(LocalDateTime.now());
        try {
            entityManager.persist(review);
            entityManager.flush();
        } catch (final PersistenceException exception) {
            return Optional.empty();
        }
        return findReviewById(review.getId());
    }

    @Override
    public Optional<ReviewModel> findReviewByBookingReviewerAndTargetType(
            final int bookingId, final int reviewerUserId, final ReviewTargetType targetType) {
        final TypedQuery<ReviewRowOrm> query = entityManager
                .createQuery(
                        ROW_SELECT
                                + " WHERE b.id = :bookingId AND sender.id = :senderId"
                                + " AND r.targetType = :targetType",
                        ReviewRowOrm.class)
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("bookingId", bookingId)
                .setParameter("senderId", reviewerUserId)
                .setParameter("targetType", TargetEnumOrm.valueOf(targetType.name()));
        return query.getResultList().stream().findAny().map(ReviewHibernateDao::mapRow);
    }

    @Override
    public List<ReviewModel> listReviewsByTarget(final ReviewTargetType targetType, final int targetId) {
        return mapRows(targetQuery(targetType, targetId, -1));
    }

    @Override
    public List<ReviewModel> listLatestReviewsByTarget(
            final ReviewTargetType targetType, final int targetId, final int limit) {
        final int safeLimit = Math.max(1, limit);
        return mapRows(targetQuery(targetType, targetId, safeLimit));
    }

    @Override
    public List<ReviewModel> listReviewsByReviewer(final int reviewerUserId) {
        final TypedQuery<ReviewRowOrm> query = entityManager
                .createQuery(
                        ROW_SELECT + " WHERE sender.id = :senderId ORDER BY r.createdAt DESC, r.id DESC",
                        ReviewRowOrm.class)
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("senderId", reviewerUserId);
        return mapRows(query.getResultList());
    }

    @Override
    public List<ReviewModel> listReviewsByReviewee(final int revieweeUserId) {
        final TypedQuery<ReviewRowOrm> query = entityManager
                .createQuery(
                        ROW_SELECT + " WHERE b.guest.id = :guestId ORDER BY r.createdAt DESC, r.id DESC",
                        ReviewRowOrm.class)
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("guestId", revieweeUserId);
        return mapRows(query.getResultList());
    }

    @Override
    public Optional<ReviewModel> findReviewById(final int reviewId) {
        final TypedQuery<ReviewRowOrm> query = entityManager
                .createQuery(ROW_SELECT + " WHERE r.id = :reviewId", ReviewRowOrm.class)
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("reviewId", reviewId);
        return query.getResultList().stream().findAny().map(ReviewHibernateDao::mapRow);
    }

    @Override
    @Transactional
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return entityManager
                        .createQuery("DELETE FROM ReviewOrm r WHERE r.id = :id AND r.sender.id = :senderId")
                        .setParameter("id", reviewId)
                        .setParameter("senderId", reviewerUserId)
                        .executeUpdate()
                > 0;
    }

    @Override
    public RatingSummaryModel ratingSummaryByTarget(final ReviewTargetType targetType, final int targetId) {
        final String hql = "SELECT COALESCE(AVG(r.rating), 0), COUNT(r) FROM ReviewOrm r"
                + " JOIN r.booking b JOIN b.version v JOIN v.item i"
                + " WHERE r.targetType = :targetType"
                + " AND (CASE WHEN r.targetType = :" + ITEM_TARGET_PARAM
                + " THEN i.id ELSE b.guest.id END) = :targetId";
        final Object[] row = (Object[]) entityManager
                .createQuery(hql)
                .setParameter("targetType", TargetEnumOrm.valueOf(targetType.name()))
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("targetId", targetId)
                .getSingleResult();
        final double average = row[0] == null ? 0d : ((Number) row[0]).doubleValue();
        final long total = row[1] == null ? 0L : ((Number) row[1]).longValue();
        return new RatingSummaryModel(average, total);
    }

    private List<ReviewRowOrm> targetQuery(
            final ReviewTargetType targetType, final int targetId, final int maxResults) {
        final TypedQuery<ReviewRowOrm> query = entityManager
                .createQuery(
                        ROW_SELECT
                                + " WHERE r.targetType = :targetType"
                                + " AND (CASE WHEN r.targetType = :" + ITEM_TARGET_PARAM
                                + " THEN i.id ELSE b.guest.id END) = :targetId"
                                + " ORDER BY r.createdAt DESC, r.id DESC",
                        ReviewRowOrm.class)
                .setParameter(ITEM_TARGET_PARAM, TargetEnumOrm.ITEM)
                .setParameter("targetType", TargetEnumOrm.valueOf(targetType.name()))
                .setParameter("targetId", targetId);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    private static List<ReviewModel> mapRows(final List<ReviewRowOrm> rows) {
        final List<ReviewModel> reviews = new ArrayList<>(rows.size());
        for (final ReviewRowOrm row : rows) {
            reviews.add(mapRow(row));
        }
        return reviews;
    }

    private static ReviewModel mapRow(final ReviewRowOrm row) {
        final ReviewModel review = new ReviewModel();
        review.setId(row.getId());
        review.setBookingId(row.getBookingId());
        review.setSenderId(row.getSenderId());
        final TargetEnumOrm target = row.getTargetType();
        review.setTargetType(target == null ? null : ReviewTargetType.valueOf(target.name()));
        review.setTargetId(row.getTargetId());
        review.setRating(row.getRating());
        review.setComment(row.getComment());
        final LocalDateTime createdAt = row.getCreatedAt();
        review.setCreatedAt(
                createdAt == null
                        ? null
                        : createdAt.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        return review;
    }
}
