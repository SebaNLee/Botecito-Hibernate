package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewJdbcDao implements ReviewDao {

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ReviewJdbcDao(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public Optional<Review> createReview(
            final int bookingId,
            final int reviewerUserId,
            final int revieweeUserId,
            final ReviewTargetType targetType,
            final int targetId,
            final int rating,
            final String reviewComment) {
        if (postgresDialect) {
            final String insertSql = "INSERT INTO review"
                    + " (booking_id, sender_id, target_type, rating, comment)"
                    + " VALUES (?, ?, CAST(? AS target_enum), ?, ?)"
                    + " RETURNING id";
            final Object[] insertArgs = {bookingId, reviewerUserId, targetType.name(), rating, reviewComment};
            final Integer insertedId = jdbcTemplate.queryForObject(insertSql, Integer.class, insertArgs);
            return findReviewById(java.util.Objects.requireNonNull(insertedId, "review insert returned no id")
                    .intValue());
        }

        if (findReviewByBookingReviewerAndTargetType(bookingId, reviewerUserId, targetType)
                .isPresent()) {
            return Optional.empty();
        }
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("review").usingGeneratedKeyColumns("id");
        final Map<String, Object> args = new HashMap<>();
        args.put("booking_id", bookingId);
        args.put("sender_id", reviewerUserId);
        args.put("target_type", targetType.name());
        args.put("rating", rating);
        args.put("comment", reviewComment);
        try {
            final Number key = insert.executeAndReturnKey(args);
            return findReviewById(key.intValue());
        } catch (final DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Review> findReviewByBookingReviewerAndTargetType(
            final int bookingId, final int reviewerUserId, final ReviewTargetType targetType) {
        return jdbcTemplate
                .query(
                        "SELECT r.*, b.guest_id AS reviewee_user_id,"
                                + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                                + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                                + " WHERE r.booking_id = ? AND r.sender_id = ? AND "
                                + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS,
                        ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                        bookingId,
                        reviewerUserId,
                        targetType.name())
                .stream()
                .findAny();
    }

    @Override
    public List<Review> listReviewsByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT r.*, b.guest_id AS reviewee_user_id,"
                        + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                        + " WHERE " + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS
                        + " AND (CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END) = ? ORDER BY r.created_at DESC, r.id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId);
    }

    @Override
    public List<Review> listLatestReviewsByTarget(
            final ReviewTargetType targetType, final int targetId, final int limit) {
        final int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(
                "SELECT r.*, b.guest_id AS reviewee_user_id,"
                        + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                        + " WHERE " + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS
                        + " AND (CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END) = ? ORDER BY r.created_at DESC, r.id DESC LIMIT ?",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId,
                safeLimit);
    }

    @Override
    public List<Review> listReviewsByReviewer(final int reviewerUserId) {
        return jdbcTemplate.query(
                "SELECT r.*, b.guest_id AS reviewee_user_id,"
                        + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                        + " WHERE r.sender_id = ? ORDER BY r.created_at DESC, r.id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                reviewerUserId);
    }

    @Override
    public List<Review> listReviewsByReviewee(final int revieweeUserId) {
        return jdbcTemplate.query(
                "SELECT r.*, b.guest_id AS reviewee_user_id,"
                        + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                        + " WHERE b.guest_id = ? ORDER BY r.created_at DESC, r.id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                revieweeUserId);
    }

    @Override
    public Optional<Review> findReviewById(final int reviewId) {
        return jdbcTemplate
                .query(
                        "SELECT r.*, b.guest_id AS reviewee_user_id,"
                                + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id"
                                + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                                + " WHERE r.id = ?",
                        ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                        reviewId)
                .stream()
                .findAny();
    }

    @Override
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return jdbcTemplate.update("DELETE FROM review WHERE id = ? AND sender_id = ?", reviewId, reviewerUserId) > 0;
    }

    @Override
    public RatingSummary ratingSummaryByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT COALESCE(AVG(r.rating), 0) AS avg_rating, COUNT(*) AS total_reviews"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id JOIN version v ON v.id = b.version_id"
                        + " WHERE " + ItemPersistenceSql.REVIEW_R_TARGET_TYPE_EQUALS
                        + " AND (CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END) = ?",
                rs -> {
                    if (!rs.next()) {
                        return RatingSummary.empty();
                    }
                    return new RatingSummary(rs.getDouble("avg_rating"), rs.getInt("total_reviews"));
                },
                targetType.name(),
                targetId);
    }
}
