package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemBookingJdbcDao implements ItemBookingDao {

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ItemBookingJdbcDao(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public List<ItemBooking> listBookings() {
        return jdbcTemplate.query(
                "SELECT b.*, v.item_id AS item_id FROM booking b JOIN \"version\" v ON v.id = b.version_id ORDER BY b.id",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER);
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT b.*, v.item_id AS item_id FROM booking b JOIN \"version\" v ON v.id = b.version_id WHERE v.item_id = ? ORDER BY b.id",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                itemId);
    }

    @Override
    public List<ItemBooking> listBookingsByGuestId(final int guestId) {
        return jdbcTemplate.query(
                ItemPersistenceSql.BOOKING_BY_GUEST_BASE + " ORDER BY b.created_at DESC, b.id DESC",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                guestId);
    }

    @Override
    public List<ItemBooking> listBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "");
    }

    @Override
    public List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "b.status = 'PENDING'");
    }

    @Override
    public List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "b.status = 'PAID'");
    }

    private List<ItemBooking> listBookingsForOwnerFiltered(final int ownerId, final String andClause) {
        final String sql = ItemPersistenceSql.BOOKING_BY_OWNER_BASE
                + (andClause.isEmpty() ? "" : " AND " + andClause)
                + " ORDER BY b.created_at DESC, b.id DESC";
        return jdbcTemplate.query(sql, ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER, ownerId);
    }

    @Override
    public List<ItemBooking> listActiveBookingsByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT b.*, v.item_id AS item_id"
                        + ItemPersistenceSql.ITEM_BOOKING_VERSION_JOIN
                        + " AND b.status IN (" + ItemPersistenceSql.EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> i.host_id)"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                itemId);
    }

    @Override
    public Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken) {
        // Host-decision tokens were removed from the new schema.
        return Optional.empty();
    }

    @Override
    public List<ItemBooking> findBookingsByHostDecisionTokens(final Collection<String> hostDecisionTokens) {
        if (hostDecisionTokens == null || hostDecisionTokens.isEmpty()) {
            return List.of();
        }
        return List.of();
    }

    @Override
    public Optional<ItemBooking> findBookingById(final int bookingId) {
        return jdbcTemplate
                .query(
                        "SELECT b.*, v.item_id AS item_id FROM booking b JOIN \"version\" v ON v.id = b.version_id WHERE b.id = ?",
                        ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                        bookingId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId) {
        return querySnapshotOptional(
                ItemPersistenceSql.SNAPSHOT_SELECT + ItemPersistenceSql.SNAPSHOT_BOOKING_JOIN + "b.guest_id = ?",
                bookingId,
                guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId) {
        return querySnapshotOptional(
                ItemPersistenceSql.SNAPSHOT_SELECT + ItemPersistenceSql.SNAPSHOT_BOOKING_JOIN + "i.host_id = ?",
                bookingId,
                ownerId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(
            final int versionId, final int itemId, final int guestId) {
        return querySnapshotOptional(
                "SELECT v.*, i.id AS item_id, i.host_id AS owner_id, l.name AS location, img.data AS cover_image_data"
                        + " FROM \"version\" v"
                        + " JOIN booking b ON b.version_id = v.id"
                        + " JOIN item i ON i.id = v.item_id"
                        + " JOIN location l ON l.id = v.location_id"
                        + " LEFT JOIN media m ON m.version_id = v.id AND m.index = 0"
                        + " LEFT JOIN image img ON img.id = m.image_id"
                        + " WHERE v.id = ? AND i.id = ? AND b.guest_id = ?"
                        + " LIMIT 1",
                versionId,
                itemId,
                guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(
            final int versionId, final int itemId, final int ownerId) {
        return querySnapshotOptional(
                "SELECT DISTINCT v.*, i.id AS item_id, i.host_id AS owner_id, l.name AS location, img.data AS cover_image_data"
                        + " FROM \"version\" v"
                        + " JOIN booking b ON b.version_id = v.id"
                        + " JOIN item i ON i.id = v.item_id"
                        + " JOIN location l ON l.id = v.location_id"
                        + " LEFT JOIN media m ON m.version_id = v.id AND m.index = 0"
                        + " LEFT JOIN image img ON img.id = m.image_id"
                        + " WHERE v.id = ? AND i.id = ? AND i.host_id = ?"
                        + " LIMIT 1",
                versionId,
                itemId,
                ownerId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId) {
        return querySnapshotList(
                ItemPersistenceSql.SNAPSHOT_LIST_BASE + "b.guest_id = ? ORDER BY v.created_at DESC, v.id DESC",
                itemId,
                guestId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId) {
        return querySnapshotList(
                ItemPersistenceSql.SNAPSHOT_LIST_BASE + "i.host_id = ? ORDER BY v.created_at DESC, v.id DESC",
                itemId,
                ownerId);
    }

    private Optional<ItemSnapshot> querySnapshotOptional(final @NonNull String sql, final Object... args) {
        return jdbcTemplate.query(sql, ItemJdbcRowMappers.ITEM_SNAPSHOT_ROW_MAPPER, args).stream()
                .findAny();
    }

    private List<ItemSnapshot> querySnapshotList(final @NonNull String sql, final Object... args) {
        return jdbcTemplate.query(sql, ItemJdbcRowMappers.ITEM_SNAPSHOT_ROW_MAPPER, args);
    }

    @Override
    public ItemBooking createBookingRequest(
            final int itemId,
            final int guestId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String requestMessage,
            final String hostDecisionToken) {
        final int id;
        if (postgresDialect) {
            id = Objects.requireNonNull(
                    jdbcTemplate.queryForObject(
                            "INSERT INTO booking"
                                    + " (version_id, guest_id, start, \"end\", status, msg)"
                                    + " VALUES ((SELECT MAX(v.id) FROM \"version\" v WHERE v.item_id = ?), ?, ?, ?, ?::booking_status_enum, ?)"
                                    + " RETURNING id",
                            Integer.class,
                            itemId,
                            guestId,
                            Timestamp.from(startTime.toInstant()),
                            Timestamp.from(endTime.toInstant()),
                            toDbBookingStatus(BookingState.BOOKING_PENDING),
                            requestMessage),
                    "Could not create booking for item " + itemId);
        } else {
            final Map<String, Object> args = new HashMap<>();
            args.put(
                    "version_id",
                    jdbcTemplate.queryForObject(
                            "SELECT MAX(id) FROM \"version\" WHERE item_id = ?", Integer.class, itemId));
            args.put("guest_id", guestId);
            args.put("start", Timestamp.from(startTime.toInstant()));
            args.put("end", Timestamp.from(endTime.toInstant()));
            args.put("status", toDbBookingStatus(BookingState.BOOKING_PENDING));
            args.put("msg", requestMessage);
            id = insertItemBookingReturningIdHsql(args);
        }
        return findBookingById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted booking " + id));
    }

    @Override
    public ItemBooking insertOwnerPersonalBlock(
            final int itemId,
            final int ownerId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String hostDecisionToken,
            final OffsetDateTime hostDecisionRecordedAt) {
        final int id;
        if (postgresDialect) {
            id = Objects.requireNonNull(
                    jdbcTemplate.queryForObject(
                            "INSERT INTO booking"
                                    + " (version_id, guest_id, start, \"end\", status, msg)"
                                    + " VALUES ((SELECT MAX(v.id) FROM \"version\" v WHERE v.item_id = ?), ?, ?, ?, ?::booking_status_enum, NULL)"
                                    + " RETURNING id",
                            Integer.class,
                            itemId,
                            ownerId,
                            Timestamp.from(startTime.toInstant()),
                            Timestamp.from(endTime.toInstant()),
                            toDbBookingStatus(BookingState.BOOKING_CONFIRMED)),
                    "Could not create owner personal block for item " + itemId);
        } else {
            final Map<String, Object> args = new HashMap<>();
            args.put(
                    "version_id",
                    jdbcTemplate.queryForObject(
                            "SELECT MAX(id) FROM \"version\" WHERE item_id = ?", Integer.class, itemId));
            args.put("guest_id", ownerId);
            args.put("start", Timestamp.from(startTime.toInstant()));
            args.put("end", Timestamp.from(endTime.toInstant()));
            args.put("status", toDbBookingStatus(BookingState.BOOKING_CONFIRMED));
            args.put("msg", null);
            id = insertItemBookingReturningIdHsql(args);
        }
        return findBookingById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted personal block booking " + id));
    }

    private int insertItemBookingReturningIdHsql(final Map<String, Object> columns) {
        final Timestamp now = Timestamp.from(Instant.now());
        columns.putIfAbsent("created_at", now);
        columns.putIfAbsent("updated_at", now);
        jdbcTemplate.update(
                "INSERT INTO booking (version_id, guest_id, start, \"end\", status, msg, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                columns.get("version_id"),
                columns.get("guest_id"),
                columns.get("start"),
                columns.get("end"),
                columns.get("status"),
                columns.get("msg"),
                columns.get("created_at"),
                columns.get("updated_at"));
        return Objects.requireNonNull(
                jdbcTemplate.queryForObject("CALL IDENTITY()", Integer.class),
                "Could not read inserted booking id in HSQL");
    }

    @Override
    public boolean markBookingCancelled(final int bookingId) {
        return jdbcTemplate.update(
                        "UPDATE booking"
                                + " SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ?"
                                + " AND status = 'ACCEPTED'",
                        bookingId)
                > 0;
    }

    @Override
    public boolean deleteOwnerSelfBlock(final int bookingId, final int ownerId) {
        return jdbcTemplate.update(
                        "DELETE FROM booking WHERE id = ? AND guest_id = ? AND status = 'ACCEPTED'", bookingId, ownerId)
                > 0;
    }

    @Override
    public boolean resolveBookingByHostDecisionToken(
            final String hostDecisionToken, final BookingState newState, final OffsetDateTime hostDecisionUsedAt) {
        // Host-decision tokens were removed from the new schema.
        return false;
    }

    @Override
    public void expireAllDueBookings(final OffsetDateTime startTimeThreshold) {
        jdbcTemplate.update(
                "UPDATE booking"
                        + " SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP"
                        + " WHERE status NOT IN ('FINISHED', 'CONFIRMED', 'CANCELLED')"
                        + " AND start < ?",
                Timestamp.from(startTimeThreshold.toInstant()));
    }

    @Override
    public int resolveBookingsByHostDecisionTokens(
            final Collection<String> hostDecisionTokens,
            final BookingState newState,
            final OffsetDateTime hostDecisionUsedAt) {
        if (hostDecisionTokens == null || hostDecisionTokens.isEmpty()) {
            return 0;
        }
        return 0;
    }

    @Override
    public BookingPaymentProof createPaymentProof(
            final int bookingId,
            final int uploaderId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply) {
        final int id;
        if (postgresDialect) {
            id = Objects.requireNonNull(
                    jdbcTemplate.queryForObject(
                            "INSERT INTO payment_proof"
                                    + " (booking_id, filename, content_type, file_data, reply_msg)"
                                    + " VALUES (?, ?, ?, ?, ?)"
                                    + " RETURNING id",
                            Integer.class,
                            bookingId,
                            fileName,
                            contentType,
                            fileData,
                            guestReply),
                    "Could not create payment proof for booking " + bookingId);
        } else {
            final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName("payment_proof")
                    .usingGeneratedKeyColumns("id");
            final Map<String, Object> values = new HashMap<>();
            values.put("booking_id", bookingId);
            values.put("filename", fileName);
            values.put("content_type", contentType);
            values.put("file_data", fileData);
            values.put("reply_msg", guestReply);
            id = insert.executeAndReturnKey(values).intValue();
        }
        return findPaymentProofById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted payment proof " + id));
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId) {
        return jdbcTemplate
                .query(
                        "SELECT id, booking_id, NULL AS uploader_id, filename, content_type, file_data, created_at, refuse_msg, refused_at, reply_msg FROM payment_proof WHERE booking_id = ? ORDER BY id DESC LIMIT 1",
                        ItemJdbcRowMappers.BOOKING_PAYMENT_PROOF_ROW_MAPPER,
                        bookingId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofById(final int proofId) {
        return jdbcTemplate
                .query(
                        "SELECT id, booking_id, NULL AS uploader_id, filename, content_type, file_data, created_at, refuse_msg, refused_at, reply_msg FROM payment_proof WHERE id = ?",
                        ItemJdbcRowMappers.BOOKING_PAYMENT_PROOF_ROW_MAPPER,
                        proofId)
                .stream()
                .findAny();
    }

    @Override
    public boolean deletePaymentProofByBookingId(final int bookingId) {
        return jdbcTemplate.update("DELETE FROM payment_proof WHERE booking_id = ?", bookingId) > 0;
    }

    @Override
    public boolean markBookingPaymentSubmitted(final int bookingId, final int guestId) {
        return updateGuestBookingStateFrom(
                bookingId, guestId, BookingState.BOOKING_CONFIRMED, BookingState.BOOKING_PAYMENT_SUBMITTED);
    }

    @Override
    public boolean markBookingPaymentResubmitted(final int bookingId, final int guestId) {
        return updateGuestBookingStateFrom(
                bookingId, guestId, BookingState.BOOKING_PAYMENT_REFUSED, BookingState.BOOKING_PAYMENT_SUBMITTED);
    }

    @Override
    public boolean markBookingPaymentRefused(final int bookingId, final int ownerId, final String reason) {
        if (!updateOwnerBookingFromPaymentSubmitted(bookingId, ownerId, BookingState.BOOKING_PAYMENT_REFUSED)) {
            return false;
        }
        jdbcTemplate.update(
                "UPDATE payment_proof"
                        + " SET refuse_msg = ?, refused_at = CURRENT_TIMESTAMP"
                        + " WHERE booking_id = ?",
                reason,
                bookingId);
        return true;
    }

    @Override
    public boolean markBookingPaid(final int bookingId, final int ownerId) {
        return updateOwnerBookingFromPaymentSubmitted(bookingId, ownerId, BookingState.BOOKING_PAID);
    }

    private boolean updateGuestBookingStateFrom(
            final int bookingId, final int guestId, final BookingState requiredState, final BookingState newState) {
        final String sql = postgresDialect
                ? "UPDATE booking"
                        + " SET status = ?::booking_status_enum, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE id = ? AND guest_id = ? AND status = ?::booking_status_enum"
                : "UPDATE booking"
                        + " SET status = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE id = ? AND guest_id = ? AND status = ?";
        return jdbcTemplate.update(
                        sql, toDbBookingStatus(newState), bookingId, guestId, toDbBookingStatus(requiredState))
                > 0;
    }

    private boolean updateOwnerBookingFromPaymentSubmitted(
            final int bookingId, final int ownerId, final BookingState newState) {
        if (postgresDialect) {
            return jdbcTemplate.update(
                            ItemPersistenceSql.BOOKING_UPDATE_BY_OWNER_SUBMITTED,
                            toDbBookingStatus(newState),
                            bookingId,
                            ownerId)
                    > 0;
        }
        return jdbcTemplate.update(
                        "UPDATE booking b"
                                + " SET status = ?, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE b.id = ?"
                                + " AND b.status = 'PAID'"
                                + " AND EXISTS ("
                                + "   SELECT 1 FROM \"version\" v JOIN item i ON i.id = v.item_id"
                                + "   WHERE v.id = b.version_id AND i.host_id = ?"
                                + " )",
                        toDbBookingStatus(newState),
                        bookingId,
                        ownerId)
                > 0;
    }

    private static String toDbBookingStatus(final BookingState state) {
        return switch (state) {
            case BOOKING_PENDING -> "PENDING";
            case BOOKING_CONFIRMED -> "ACCEPTED";
            case BOOKING_REJECTED -> "REJECTED";
            case BOOKING_CANCELLED -> "CANCELLED";
            case BOOKING_COMPLETED -> "FINISHED";
            case BOOKING_PAYMENT_SUBMITTED -> "PAID";
            case BOOKING_PAID -> "CONFIRMED";
            case BOOKING_PAYMENT_REFUSED -> "REFUSED";
        };
    }
}
