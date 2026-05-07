package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

final class ItemJdbcRowMappers {

    private ItemJdbcRowMappers() {}

    static final @NonNull RowMapper<Item> ITEM_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setOwnerId(rs.getInt("host_id"));
        item.setTypeId(rs.getInt("type_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setPricePerHour(rs.getInt("price"));
        item.setCapacityPeople(rs.getInt("capacity"));
        item.setMaxWeightKg(rs.getBigDecimal("weight"));
        item.setDifficultyLevel((Integer) rs.getObject("difficulty"));
        item.setLocationOptionId((Integer) rs.getObject("location_id"));
        item.setLocation(rs.getString("location"));
        item.setActive("ACTIVE".equals(rs.getString("status")));
        item.setOwnerDeleteToken(null);
        item.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        return item;
    };

    static final @NonNull RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final User user = new User();
        user.setId(rs.getInt("id"));
        user.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        user.setGivenName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPaymentAlias(rs.getString("alias"));
        user.setPreferredLanguage(PreferredLanguage.fromPersistence(rs.getString("language")));
        return user;
    };

    static final @NonNull RowMapper<ItemType> ITEM_TYPE_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemType itemType = new ItemType();
        itemType.setId(rs.getInt("id"));
        itemType.setName(rs.getString("name"));
        return itemType;
    };

    static final @NonNull RowMapper<ItemAvailability> ITEM_AVAILABILITY_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemAvailability availability = new ItemAvailability();
        availability.setId(rs.getInt("id"));
        availability.setItemId(rs.getInt("item_id"));
        final String weekdayName = rs.getString("weekday");
        availability.setWeekday(weekdayName == null ? null : DayOfWeek.valueOf(weekdayName));
        final Time startSql = rs.getTime("start_time");
        availability.setStartTime(startSql == null ? null : startSql.toLocalTime());
        final Time endSql = rs.getTime("end_time");
        availability.setEndTime(endSql == null ? null : endSql.toLocalTime());
        return availability;
    };

    static final @NonNull RowMapper<ItemBooking> ITEM_BOOKING_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemBooking booking = new ItemBooking();
        booking.setId(rs.getInt("id"));
        booking.setItemId(rs.getInt("item_id"));
        booking.setGuestId(rs.getInt("guest_id"));
        booking.setStartTime(readOffsetDateTime(rs, "start"));
        booking.setEndTime(readOffsetDateTime(rs, "end"));
        booking.setState(toLegacyBookingState(rs.getString("status")));
        booking.setRequestMessage(rs.getString("msg"));
        booking.setHostDecisionToken(null);
        booking.setHostDecisionUsedAt(null);
        booking.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        booking.setUpdatedAt(readOffsetDateTime(rs, "updated_at"));
        return booking;
    };

    static final @NonNull RowMapper<ItemSnapshot> ITEM_SNAPSHOT_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setVersionId(rs.getInt("id"));
        snapshot.setId(rs.getInt("item_id"));
        snapshot.setOwnerId(rs.getInt("owner_id"));
        snapshot.setTypeId(rs.getInt("type_id"));
        snapshot.setTitle(rs.getString("title"));
        snapshot.setDescription(rs.getString("description"));
        snapshot.setPricePerHour(rs.getInt("price"));
        snapshot.setCapacityPeople(rs.getInt("capacity"));
        snapshot.setMaxWeightKg(rs.getBigDecimal("weight"));
        snapshot.setDifficultyLevel((Integer) rs.getObject("difficulty"));
        snapshot.setLocationOptionId((Integer) rs.getObject("location_id"));
        snapshot.setLocation(rs.getString("location"));
        snapshot.setCoverImageData(rs.getBytes("cover_image_data"));
        snapshot.setSnapshotCreatedAt(readOffsetDateTime(rs, "created_at"));
        return snapshot;
    };

    static final @NonNull RowMapper<BookingPaymentProof> BOOKING_PAYMENT_PROOF_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> {
                final BookingPaymentProof proof = new BookingPaymentProof();
                proof.setId(rs.getInt("id"));
                proof.setBookingId(rs.getInt("booking_id"));
                proof.setUploaderId((Integer) rs.getObject("uploader_id"));
                proof.setFileName(rs.getString("filename"));
                proof.setContentType(rs.getString("content_type"));
                proof.setFileData(rs.getBytes("file_data"));
                proof.setCreatedAt(readOffsetDateTime(rs, "created_at"));
                proof.setRefusalReason(rs.getString("refuse_msg"));
                proof.setRefusedAt(readOffsetDateTime(rs, "refused_at"));
                proof.setGuestReply(rs.getString("reply_msg"));
                return proof;
            };

    static final @NonNull RowMapper<Review> REVIEW_ROW_MAPPER = (ResultSet rs, int rowNum) -> new Review(
            readRequiredIntColumn(rs, "id"),
            readRequiredIntColumn(rs, "booking_id"),
            readRequiredIntColumn(rs, "sender_id"),
            readRequiredIntColumn(rs, "reviewee_user_id"),
            ReviewTargetType.valueOf(rs.getString("target_type")),
            readRequiredIntColumn(rs, "target_id"),
            readRequiredIntColumn(rs, "rating"),
            rs.getString("comment"),
            readOffsetDateTime(rs, "created_at"),
            readOffsetDateTime(rs, "created_at"));

    private static BookingState toLegacyBookingState(final String status) {
        return switch (status) {
            case "PENDING" -> BookingState.BOOKING_PENDING;
            case "ACCEPTED" -> BookingState.BOOKING_CONFIRMED;
            case "REJECTED" -> BookingState.BOOKING_REJECTED;
            case "CANCELLED" -> BookingState.BOOKING_CANCELLED;
            case "FINISHED" -> BookingState.BOOKING_COMPLETED;
            case "PAID" -> BookingState.BOOKING_PAYMENT_SUBMITTED;
            case "CONFIRMED" -> BookingState.BOOKING_PAID;
            case "REFUSED" -> BookingState.BOOKING_PAYMENT_REFUSED;
            default -> throw new IllegalArgumentException("Unknown booking status: " + status);
        };
    }

    static Integer readRequiredIntColumn(final ResultSet rs, final String column) throws SQLException {
        final int value = rs.getInt(column);
        if (rs.wasNull()) {
            throw new IllegalStateException("Expected non-null integer column: " + column);
        }
        return value;
    }

    static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String column) throws SQLException {
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

    static Timestamp toTimestamp(final Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return Timestamp.from(offsetDateTime.toInstant());
        }
        throw new IllegalStateException("Expected timestamp-compatible value");
    }
}
