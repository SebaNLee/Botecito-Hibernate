package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        return jdbcTemplate.query("SELECT * FROM item_booking ORDER BY id", ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER);
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_booking WHERE item_id = ? ORDER BY id",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                itemId);
    }

    @Override
    public List<ItemBooking> listBookingsByGuestId(final int guestId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_booking WHERE guest_id = ? ORDER BY created_at DESC, id DESC",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                guestId);
    }

    @Override
    public List<ItemBooking> listBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "");
    }

    @Override
    public List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "b.state = 'BOOKING_PENDING'");
    }

    @Override
    public List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId) {
        return listBookingsForOwnerFiltered(ownerId, "b.state = 'BOOKING_PAYMENT_SUBMITTED'");
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
                "SELECT b.*"
                        + ItemPersistenceSql.ITEM_BOOKING_VERSION_JOIN
                        + " AND b.state IN (" + ItemPersistenceSql.EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> v.owner_id)"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                itemId);
    }

    @Override
    public Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_booking WHERE host_decision_token = ?",
                        ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                        hostDecisionToken)
                .stream()
                .findAny();
    }

    @Override
    public List<ItemBooking> findBookingsByHostDecisionTokens(final Collection<String> hostDecisionTokens) {
        if (hostDecisionTokens == null || hostDecisionTokens.isEmpty()) {
            return List.of();
        }
        final String placeholders = String.join(", ", Collections.nCopies(hostDecisionTokens.size(), "?"));
        return jdbcTemplate.query(
                "SELECT * FROM item_booking WHERE host_decision_token IN (" + placeholders + ")",
                ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER,
                hostDecisionTokens.toArray());
    }

    @Override
    public Optional<ItemBooking> findBookingById(final int bookingId) {
        return jdbcTemplate
                .query("SELECT * FROM item_booking WHERE id = ?", ItemJdbcRowMappers.ITEM_BOOKING_ROW_MAPPER, bookingId)
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
                ItemPersistenceSql.SNAPSHOT_SELECT + ItemPersistenceSql.SNAPSHOT_BOOKING_JOIN + "v.owner_id = ?",
                bookingId,
                ownerId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(
            final int versionId, final int itemId, final int guestId) {
        return querySnapshotOptional(
                "SELECT v.*, b.item_id"
                        + " FROM item_publication_version v"
                        + " JOIN item_booking b ON b.item_version_id = v.id"
                        + " WHERE v.id = ? AND b.item_id = ? AND b.guest_id = ?"
                        + " LIMIT 1",
                versionId,
                itemId,
                guestId);
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(
            final int versionId, final int itemId, final int ownerId) {
        return querySnapshotOptional(
                "SELECT DISTINCT v.*, b.item_id"
                        + " FROM item_publication_version v"
                        + " JOIN item_booking b ON b.item_version_id = v.id"
                        + " WHERE v.id = ? AND b.item_id = ? AND v.owner_id = ?"
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
                ItemPersistenceSql.SNAPSHOT_LIST_BASE + "v.owner_id = ? ORDER BY v.created_at DESC, v.id DESC",
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
                            "INSERT INTO item_booking"
                                    + " (item_id, guest_id, start_time, end_time, state, request_message, host_decision_token)"
                                    + " VALUES (?, ?, ?, ?, ?::booking_state, ?, ?)"
                                    + " RETURNING id",
                            Integer.class,
                            itemId,
                            guestId,
                            Timestamp.from(startTime.toInstant()),
                            Timestamp.from(endTime.toInstant()),
                            BookingState.BOOKING_PENDING.name(),
                            requestMessage,
                            hostDecisionToken),
                    "Could not create booking for item " + itemId);
        } else {
            final Map<String, Object> args = new HashMap<>();
            args.put("item_id", itemId);
            args.put("guest_id", guestId);
            args.put("start_time", Timestamp.from(startTime.toInstant()));
            args.put("end_time", Timestamp.from(endTime.toInstant()));
            args.put("state", BookingState.BOOKING_PENDING.name());
            args.put("request_message", requestMessage);
            args.put("host_decision_token", hostDecisionToken);
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
                            "INSERT INTO item_booking"
                                    + " (item_id, guest_id, start_time, end_time, state, request_message, host_decision_token, host_decision_used_at)"
                                    + " VALUES (?, ?, ?, ?, ?::booking_state, NULL, ?, ?)"
                                    + " RETURNING id",
                            Integer.class,
                            itemId,
                            ownerId,
                            Timestamp.from(startTime.toInstant()),
                            Timestamp.from(endTime.toInstant()),
                            BookingState.BOOKING_CONFIRMED.name(),
                            hostDecisionToken,
                            Timestamp.from(hostDecisionRecordedAt.toInstant())),
                    "Could not create owner personal block for item " + itemId);
        } else {
            final Map<String, Object> args = new HashMap<>();
            args.put("item_id", itemId);
            args.put("guest_id", ownerId);
            args.put("start_time", Timestamp.from(startTime.toInstant()));
            args.put("end_time", Timestamp.from(endTime.toInstant()));
            args.put("state", BookingState.BOOKING_CONFIRMED.name());
            args.put("request_message", null);
            args.put("host_decision_token", hostDecisionToken);
            args.put("host_decision_used_at", Timestamp.from(hostDecisionRecordedAt.toInstant()));
            id = insertItemBookingReturningIdHsql(args);
        }
        return findBookingById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted personal block booking " + id));
    }

    private int insertItemBookingReturningIdHsql(final Map<String, Object> columns) {
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item_booking").usingGeneratedKeyColumns("id");
        final Timestamp now = Timestamp.from(Instant.now());
        columns.putIfAbsent("created_at", now);
        columns.putIfAbsent("updated_at", now);
        return insert.executeAndReturnKey(columns).intValue();
    }

    @Override
    public boolean markBookingCancelled(final int bookingId) {
        return jdbcTemplate.update(
                        "UPDATE item_booking"
                                + " SET state = 'BOOKING_CANCELLED', updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ?"
                                + " AND state = 'BOOKING_CONFIRMED'",
                        bookingId)
                > 0;
    }

    @Override
    public boolean resolveBookingByHostDecisionToken(
            final String hostDecisionToken, final BookingState newState, final OffsetDateTime hostDecisionUsedAt) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET state = ?::booking_state, host_decision_used_at = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE host_decision_token = ?"
                        + " AND state = 'BOOKING_PENDING'"
                        + " AND host_decision_used_at IS NULL",
                newState.name(),
                Timestamp.from(hostDecisionUsedAt.toInstant()),
                hostDecisionToken);
        return updatedRows > 0;
    }

    @Override
    public void expireAllDueBookings(final OffsetDateTime startTimeThreshold) {
        jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET state = 'BOOKING_CANCELLED', updated_at = CURRENT_TIMESTAMP"
                        + " WHERE state NOT IN ('BOOKING_COMPLETED', 'BOOKING_PAID', 'BOOKING_CANCELLED')"
                        + " AND start_time < ?",
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
        final String placeholders = String.join(", ", Collections.nCopies(hostDecisionTokens.size(), "?"));
        final List<Object> args = new ArrayList<>();
        args.add(newState.name());
        args.add(Timestamp.from(hostDecisionUsedAt.toInstant()));
        args.addAll(hostDecisionTokens);
        return jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET state = ?::booking_state, host_decision_used_at = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE host_decision_token IN (" + placeholders + ")"
                        + " AND state = 'BOOKING_PENDING'"
                        + " AND host_decision_used_at IS NULL",
                args.toArray());
    }

    @Override
    public BookingPaymentProof createPaymentProof(
            final int bookingId,
            final int uploaderId,
            final String fileName,
            final String contentType,
            final byte[] fileData,
            final String guestReply) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO booking_payment_proof"
                                + " (booking_id, uploader_id, file_name, content_type, file_data, guest_reply)"
                                + " VALUES (?, ?, ?, ?, ?, ?)"
                                + " RETURNING id",
                        Integer.class,
                        bookingId,
                        uploaderId,
                        fileName,
                        contentType,
                        fileData,
                        guestReply),
                "Could not create payment proof for booking " + bookingId);
        return findPaymentProofById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted payment proof " + id));
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofByBookingId(final int bookingId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM booking_payment_proof WHERE booking_id = ? ORDER BY id DESC LIMIT 1",
                        ItemJdbcRowMappers.BOOKING_PAYMENT_PROOF_ROW_MAPPER,
                        bookingId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofById(final int proofId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM booking_payment_proof WHERE id = ?",
                        ItemJdbcRowMappers.BOOKING_PAYMENT_PROOF_ROW_MAPPER,
                        proofId)
                .stream()
                .findAny();
    }

    @Override
    public boolean deletePaymentProofByBookingId(final int bookingId) {
        return jdbcTemplate.update("DELETE FROM booking_payment_proof WHERE booking_id = ?", bookingId) > 0;
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
                "UPDATE booking_payment_proof"
                        + " SET refusal_reason = ?, refused_at = CURRENT_TIMESTAMP"
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
        return jdbcTemplate.update(
                        "UPDATE item_booking"
                                + " SET state = ?::booking_state, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND guest_id = ? AND state = ?::booking_state",
                        newState.name(),
                        bookingId,
                        guestId,
                        requiredState.name())
                > 0;
    }

    private boolean updateOwnerBookingFromPaymentSubmitted(
            final int bookingId, final int ownerId, final BookingState newState) {
        return jdbcTemplate.update(
                        ItemPersistenceSql.BOOKING_UPDATE_BY_OWNER_SUBMITTED, newState.name(), bookingId, ownerId)
                > 0;
    }
}
