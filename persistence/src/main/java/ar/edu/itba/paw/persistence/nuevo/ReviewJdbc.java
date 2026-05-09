package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.ReviewTargetType;
import ar.edu.itba.paw.persistence.JdbcDialect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Repository;

// @Repository
public final class ReviewJdbc implements ReviewDao {

    private static final String BASE_SELECT = "SELECT r.id, r.booking_id, r.sender_id, r.target_type,"
            + " CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END AS target_id,"
            + " r.rating, r.comment, r.created_at"
            + " FROM review r JOIN booking b ON b.id = r.booking_id"
            + " JOIN version v ON v.id = b.version_id";

    private static final String TARGET_FILTER = " WHERE CAST(r.target_type AS VARCHAR(16)) = ?"
            + " AND (CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END) = ?";

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ReviewJdbc(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public Optional<ReviewModel> createReview(
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
            return findReviewById(Objects.requireNonNull(insertedId, "review insert returned no id")
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
    public Optional<ReviewModel> findReviewByBookingReviewerAndTargetType(
            final int bookingId, final int reviewerUserId, final ReviewTargetType targetType) {
        return jdbcTemplate
                .query(
                        BASE_SELECT
                                + " WHERE r.booking_id = ? AND r.sender_id = ?"
                                + " AND CAST(r.target_type AS VARCHAR(16)) = ?",
                        ReviewRowMapper.REVIEW_ROW_MAPPER,
                        bookingId,
                        reviewerUserId,
                        targetType.name())
                .stream()
                .findAny();
    }

    @Override
    public List<ReviewModel> listReviewsByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                BASE_SELECT + TARGET_FILTER + " ORDER BY r.created_at DESC, r.id DESC",
                ReviewRowMapper.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId);
    }

    @Override
    public List<ReviewModel> listLatestReviewsByTarget(
            final ReviewTargetType targetType, final int targetId, final int limit) {
        final int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(
                BASE_SELECT + TARGET_FILTER + " ORDER BY r.created_at DESC, r.id DESC LIMIT ?",
                ReviewRowMapper.REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId,
                safeLimit);
    }

    @Override
    public List<ReviewModel> listReviewsByReviewer(final int reviewerUserId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE r.sender_id = ? ORDER BY r.created_at DESC, r.id DESC",
                ReviewRowMapper.REVIEW_ROW_MAPPER,
                reviewerUserId);
    }

    @Override
    public List<ReviewModel> listReviewsByReviewee(final int revieweeUserId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE b.guest_id = ? ORDER BY r.created_at DESC, r.id DESC",
                ReviewRowMapper.REVIEW_ROW_MAPPER,
                revieweeUserId);
    }

    @Override
    public Optional<ReviewModel> findReviewById(final int reviewId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE r.id = ?", ReviewRowMapper.REVIEW_ROW_MAPPER, reviewId).stream()
                .findAny();
    }

    @Override
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return jdbcTemplate.update("DELETE FROM review WHERE id = ? AND sender_id = ?", reviewId, reviewerUserId) > 0;
    }

    @Override
    public RatingSummaryModel ratingSummaryByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT COALESCE(AVG(r.rating), 0) AS avg_rating, COUNT(*) AS total_reviews"
                        + " FROM review r JOIN booking b ON b.id = r.booking_id"
                        + " JOIN version v ON v.id = b.version_id"
                        + " WHERE CAST(r.target_type AS VARCHAR(16)) = ?"
                        + " AND (CASE WHEN r.target_type = 'ITEM' THEN v.item_id ELSE b.guest_id END) = ?",
                rs -> {
                    if (!rs.next()) {
                        return RatingSummaryModel.empty();
                    }
                    return new RatingSummaryModel(rs.getDouble("avg_rating"), rs.getLong("total_reviews"));
                },
                targetType.name(),
                targetId);
    }
}
