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
                    + " (booking_id, reviewer_user_id, reviewee_user_id, target_type, target_id, rating, comment)"
                    + " VALUES (?, ?, ?, CAST(? AS review_target_type), ?, ?, ?)"
                    + " ON CONFLICT (booking_id, reviewer_user_id, target_type) DO NOTHING"
                    + " RETURNING id";
            final Object[] insertArgs = {
                bookingId, reviewerUserId, revieweeUserId, targetType.name(), targetId, rating, reviewComment
            };
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
        args.put("reviewer_user_id", reviewerUserId);
        args.put("reviewee_user_id", revieweeUserId);
        args.put("target_type", targetType.name());
        args.put("target_id", targetId);
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
                        "SELECT * FROM review WHERE booking_id = ? AND reviewer_user_id = ? AND "
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
                "SELECT * FROM review WHERE " + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS
                        + " AND target_id = ? ORDER BY created_at DESC, id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId);
    }

    @Override
    public List<Review> listLatestReviewsByTarget(
            final ReviewTargetType targetType, final int targetId, final int limit) {
        final int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE " + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS
                        + " AND target_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId,
                safeLimit);
    }

    @Override
    public List<Review> listReviewsByReviewer(final int reviewerUserId) {
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE reviewer_user_id = ? ORDER BY created_at DESC, id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                reviewerUserId);
    }

    @Override
    public List<Review> listReviewsByReviewee(final int revieweeUserId) {
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE reviewee_user_id = ? ORDER BY created_at DESC, id DESC",
                ItemJdbcRowMappers.REVIEW_ROW_MAPPER,
                revieweeUserId);
    }

    @Override
    public Optional<Review> findReviewById(final int reviewId) {
        return jdbcTemplate
                .query("SELECT * FROM review WHERE id = ?", ItemJdbcRowMappers.REVIEW_ROW_MAPPER, reviewId)
                .stream()
                .findAny();
    }

    @Override
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return jdbcTemplate.update("DELETE FROM review WHERE id = ? AND reviewer_user_id = ?", reviewId, reviewerUserId)
                > 0;
    }

    @Override
    public RatingSummary ratingSummaryByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT COALESCE(AVG(rating), 0) AS avg_rating, COUNT(*) AS total_reviews" + " FROM review WHERE "
                        + ItemPersistenceSql.REVIEW_TARGET_TYPE_EQUALS + " AND target_id = ?",
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
