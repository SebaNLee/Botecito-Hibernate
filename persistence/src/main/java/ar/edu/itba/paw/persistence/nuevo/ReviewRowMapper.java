package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.ReviewTargetType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

public final class ReviewRowMapper {

    private ReviewRowMapper() {}

    public static final @NonNull RowMapper<ReviewModel> REVIEW_ROW_MAPPER =
            (final ResultSet rs, final int rowNum) -> mapRow(rs);

    public static @NonNull ReviewModel mapRow(final ResultSet rs) throws SQLException {
        final ReviewModel review = new ReviewModel();
        review.setId((Integer) rs.getObject("id"));
        review.setBookingId((Integer) rs.getObject("booking_id"));
        review.setSenderId((Integer) rs.getObject("sender_id"));
        final String targetType = rs.getString("target_type");
        review.setTargetType(targetType == null ? null : ReviewTargetType.valueOf(targetType));
        review.setTargetId((Integer) rs.getObject("target_id"));
        review.setRating(rs.getBigDecimal("rating"));
        review.setComment(rs.getString("comment"));
        review.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        return review;
    }

    private static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String column) throws SQLException {
        final Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        return OffsetDateTime.parse(value.toString());
    }
}
