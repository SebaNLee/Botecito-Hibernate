package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSearchSort;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJdbcDao implements ItemDao {
    private static final String EDIT_CONFLICT_BOOKING_STATES =
            "'BOOKING_PENDING', 'BOOKING_CONFIRMED', 'BOOKING_PAYMENT_SUBMITTED', 'BOOKING_PAID'";
    private static final String PUBLICATION_EDIT_SNAPSHOT_STATES =
            EDIT_CONFLICT_BOOKING_STATES + ", 'BOOKING_COMPLETED'";
    private static final String ITEM_SELECT = "SELECT i.id, i.owner_id, i.type_id, i.title, i.description,"
            + " i.price_per_hour, i.capacity_people, i.max_weight_kg, i.difficulty_level,"
            + " i.location_option_id, lo.name AS location,"
            + " i.active, i.owner_delete_token, i.created_at"
            + " FROM item i"
            + " JOIN location_option lo ON lo.id = i.location_option_id";

    private static final @NonNull RowMapper<Item> ITEM_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setOwnerId(rs.getInt("owner_id"));
        item.setTypeId(rs.getInt("type_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setPricePerHour(rs.getInt("price_per_hour"));
        item.setCapacityPeople(rs.getInt("capacity_people"));
        item.setMaxWeightKg(rs.getBigDecimal("max_weight_kg"));
        item.setDifficultyLevel((Integer) rs.getObject("difficulty_level"));
        item.setLocationOptionId((Integer) rs.getObject("location_option_id"));
        item.setLocation(rs.getString("location"));
        item.setActive(rs.getBoolean("active"));
        item.setOwnerDeleteToken(rs.getString("owner_delete_token"));
        item.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        return item;
    };

    private static final @NonNull RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final User user = new User();
        user.setId(rs.getInt("id"));
        user.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        user.setGivenName(rs.getString("given_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPaymentAlias(rs.getString("payment_alias"));
        user.setPreferredLanguage(PreferredLanguage.fromPersistence(rs.getString("preferred_language")));
        return user;
    };

    private static final @NonNull RowMapper<ItemType> ITEM_TYPE_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemType itemType = new ItemType();
        itemType.setId(rs.getInt("id"));
        itemType.setName(rs.getString("name"));
        return itemType;
    };

    private static final @NonNull RowMapper<ItemAvailability> ITEM_AVAILABILITY_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> {
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

    private static final @NonNull RowMapper<ItemBooking> ITEM_BOOKING_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemBooking booking = new ItemBooking();
        booking.setId(rs.getInt("id"));
        booking.setItemId(rs.getInt("item_id"));
        booking.setGuestId(rs.getInt("guest_id"));
        booking.setStartTime(readOffsetDateTime(rs, "start_time"));
        booking.setEndTime(readOffsetDateTime(rs, "end_time"));
        booking.setState(BookingState.valueOf(rs.getString("state")));
        booking.setRequestMessage(rs.getString("request_message"));
        booking.setHostDecisionToken(rs.getString("host_decision_token"));
        booking.setHostDecisionUsedAt(readOffsetDateTime(rs, "host_decision_used_at"));
        booking.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        booking.setUpdatedAt(readOffsetDateTime(rs, "updated_at"));
        return booking;
    };

    private static final @NonNull RowMapper<ItemSnapshot> ITEM_SNAPSHOT_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setVersionId(rs.getInt("id"));
        snapshot.setId(rs.getInt("item_id"));
        snapshot.setOwnerId(rs.getInt("owner_id"));
        snapshot.setTypeId(rs.getInt("type_id"));
        snapshot.setTitle(rs.getString("title"));
        snapshot.setDescription(rs.getString("description"));
        snapshot.setPricePerHour(rs.getInt("price_per_hour"));
        snapshot.setCapacityPeople(rs.getInt("capacity_people"));
        snapshot.setMaxWeightKg(rs.getBigDecimal("max_weight_kg"));
        snapshot.setDifficultyLevel((Integer) rs.getObject("difficulty_level"));
        snapshot.setLocationOptionId((Integer) rs.getObject("location_option_id"));
        snapshot.setLocation(rs.getString("location_name"));
        snapshot.setCoverImageData(rs.getBytes("cover_image_data"));
        snapshot.setSnapshotCreatedAt(readOffsetDateTime(rs, "created_at"));
        return snapshot;
    };

    private static final @NonNull RowMapper<BookingPaymentProof> BOOKING_PAYMENT_PROOF_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> {
                final BookingPaymentProof proof = new BookingPaymentProof();
                proof.setId(rs.getInt("id"));
                proof.setBookingId(rs.getInt("booking_id"));
                proof.setUploaderId(rs.getInt("uploader_id"));
                proof.setFileName(rs.getString("file_name"));
                proof.setContentType(rs.getString("content_type"));
                proof.setFileData(rs.getBytes("file_data"));
                proof.setCreatedAt(readOffsetDateTime(rs, "created_at"));
                proof.setRefusalReason(rs.getString("refusal_reason"));
                proof.setRefusedAt(readOffsetDateTime(rs, "refused_at"));
                proof.setGuestReply(rs.getString("guest_reply"));
                return proof;
            };

    private static final @NonNull RowMapper<Review> REVIEW_ROW_MAPPER = (ResultSet rs, int rowNum) -> new Review(
            readRequiredIntColumn(rs, "id"),
            readRequiredIntColumn(rs, "booking_id"),
            readRequiredIntColumn(rs, "reviewer_user_id"),
            readRequiredIntColumn(rs, "reviewee_user_id"),
            ReviewTargetType.valueOf(rs.getString("target_type")),
            readRequiredIntColumn(rs, "target_id"),
            readRequiredIntColumn(rs, "rating"),
            rs.getString("comment"),
            readOffsetDateTime(rs, "created_at"),
            readOffsetDateTime(rs, "updated_at"));

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ItemJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        boolean resolvedPostgresDialect = false;
        try {
            resolvedPostgresDialect =
                    Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
                        final String productName = connection.getMetaData().getDatabaseProductName();
                        return productName != null && productName.toLowerCase().contains("postgresql");
                    }));
        } catch (final RuntimeException exception) {
            resolvedPostgresDialect = false;
        }
        this.postgresDialect = resolvedPostgresDialect;
    }

    @Override
    public List<Item> listItems() {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.active = TRUE ORDER BY i.id", ITEM_ROW_MAPPER);
    }

    @Override
    public List<Item> listItems(final ItemSearchCriteria criteria, final int limit, final int offset) {
        final List<Object> args = new ArrayList<>();
        final String sql = ITEM_SELECT
                + marketplaceWhereClause(criteria, args)
                + marketplaceOrderBy(criteria)
                + " LIMIT ? OFFSET ?";
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql, ITEM_ROW_MAPPER, args.toArray());
    }

    @Override
    public int countItems(final ItemSearchCriteria criteria) {
        final List<Object> args = new ArrayList<>();
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item i"
                        + " JOIN location_option lo ON lo.id = i.location_option_id"
                        + marketplaceWhereClause(criteria, args),
                Integer.class,
                args.toArray());
        return count == null ? 0 : count;
    }

    public List<Item> listItemsByOwnerId(final int ownerId) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.owner_id = ? ORDER BY i.id DESC", ITEM_ROW_MAPPER, ownerId);
    }

    @Override
    public List<LocationOption> listLocationOptions() {
        return jdbcTemplate.query("SELECT id, name FROM location_option ORDER BY id", (rs, rowNum) -> {
            final LocationOption locationOption = new LocationOption();
            locationOption.setId(rs.getInt("id"));
            locationOption.setName(rs.getString("name"));
            return locationOption;
        });
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.id = ? AND i.active = TRUE", ITEM_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<Item> findItemByIdForOwner(final int id, final int ownerId) {
        return jdbcTemplate
                .query(ITEM_SELECT + " WHERE i.id = ? AND i.owner_id = ?", ITEM_ROW_MAPPER, id, ownerId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<Item> findAnyItemById(final int id) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.id = ?", ITEM_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<User> findUserById(final int id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<User> findUserByEmail(final String email) {
        return jdbcTemplate.query("SELECT * FROM users WHERE lower(email) = lower(?)", USER_ROW_MAPPER, email).stream()
                .findAny();
    }

    @Override
    public User createUser(
            final String givenName, final String lastName, final String email, final String preferredLanguage) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO users (given_name, last_name, email, preferred_language) VALUES (?, ?, ?, ?) RETURNING id",
                        Integer.class,
                        givenName,
                        lastName,
                        email,
                        preferredLanguage),
                "Could not create user for email " + email);

        return findUserById(id).orElseThrow(() -> new IllegalStateException("Could not read inserted user " + id));
    }

    @Override
    public boolean updateUserProfile(
            final int userId, final String givenName, final String lastName, final String preferredLanguage) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users SET given_name = ?, last_name = ?, preferred_language = ? WHERE id = ?",
                givenName,
                lastName,
                preferredLanguage,
                userId);
        return updatedRows > 0;
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return jdbcTemplate.query("SELECT * FROM item_type WHERE id = ?", ITEM_TYPE_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public boolean updatePublication(
            final int itemId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item"
                        + " SET title = ?, description = ?, price_per_hour = ?, difficulty_level = ?, location_option_id = ?"
                        + " WHERE id = ?",
                title,
                description,
                pricePerHour,
                difficultyLevel,
                locationOptionId,
                itemId);
        return updatedRows > 0;
    }

    @Override
    public boolean updatePublicationForOwner(
            final int itemId,
            final int ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item"
                        + " SET title = ?, description = ?, price_per_hour = ?, difficulty_level = ?, location_option_id = ?"
                        + " WHERE id = ? AND owner_id = ?",
                title,
                description,
                pricePerHour,
                difficultyLevel,
                locationOptionId,
                itemId,
                ownerId);
        return updatedRows > 0;
    }

    @Override
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item_booking b"
                        + " INNER JOIN item i ON i.id = b.item_id"
                        + " WHERE b.item_id = ?"
                        + " AND b.item_version_id IS NULL"
                        + " AND b.state IN (" + EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> i.owner_id)",
                Integer.class,
                itemId);
        return count != null && count > 0;
    }

    @Override
    public boolean deleteItemById(final int itemId) {
        final Optional<Item> item = findAnyItemById(itemId);
        if (item.isEmpty()) {
            return false;
        }
        if (hasBookings(itemId)) {
            if (Boolean.TRUE.equals(item.get().getActive())) {
                snapshotBookingsForPublicationEdit(itemId);
                final int updatedRows = jdbcTemplate.update("UPDATE item SET active = FALSE WHERE id = ?", itemId);
                return updatedRows > 0;
            }
            if (hasFutureRetainedBookings(itemId)) {
                return false;
            }
            final int deletedRows = jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);
            return deletedRows > 0;
        }
        final int deletedRows = jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);
        return deletedRows > 0;
    }

    @Override
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        final Optional<Item> item = findItemByIdForOwner(itemId, ownerId);
        if (item.isEmpty()) {
            return false;
        }
        if (hasBookings(itemId)) {
            if (Boolean.TRUE.equals(item.get().getActive())) {
                snapshotBookingsForPublicationEdit(itemId);
                final int updatedRows = jdbcTemplate.update(
                        "UPDATE item SET active = FALSE WHERE id = ? AND owner_id = ?", itemId, ownerId);
                return updatedRows > 0;
            }
            if (hasFutureRetainedBookings(itemId)) {
                return false;
            }
            final int deletedRows =
                    jdbcTemplate.update("DELETE FROM item WHERE id = ? AND owner_id = ?", itemId, ownerId);
            return deletedRows > 0;
        }
        final int deletedRows = jdbcTemplate.update("DELETE FROM item WHERE id = ? AND owner_id = ?", itemId, ownerId);
        return deletedRows > 0;
    }

    @Override
    public Item createItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final String ownerDeleteToken) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO item"
                                + " (owner_id, type_id, title, description, price_per_hour, capacity_people, max_weight_kg, difficulty_level, location_option_id, active, owner_delete_token)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                + " RETURNING id",
                        Integer.class,
                        ownerId,
                        typeId,
                        title,
                        description,
                        pricePerHour,
                        capacityPeople,
                        maxWeightKg,
                        difficultyLevel,
                        locationOptionId,
                        Boolean.TRUE,
                        ownerDeleteToken),
                "Could not create item for owner " + ownerId);
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.id = ?", ITEM_ROW_MAPPER, id).stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not read inserted item " + id));
    }

    @Override
    public ItemAvailability createItemAvailability(
            final int itemId, final String weekday, final String startTime, final String endTime) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO item_availability (item_id, weekday, start_time, end_time)"
                                + " VALUES (?, ?::availability_weekday, ?, ?)"
                                + " RETURNING id",
                        Integer.class,
                        itemId,
                        weekday,
                        Time.valueOf(LocalTime.parse(startTime)),
                        Time.valueOf(LocalTime.parse(endTime))),
                "Could not create availability for item " + itemId);
        return jdbcTemplate
                .query("SELECT * FROM item_availability WHERE id = ?", ITEM_AVAILABILITY_ROW_MAPPER, id)
                .stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not read inserted availability " + id));
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return jdbcTemplate.query("SELECT * FROM item_availability ORDER BY id", ITEM_AVAILABILITY_ROW_MAPPER);
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_availability WHERE item_id = ? ORDER BY id", ITEM_AVAILABILITY_ROW_MAPPER, itemId);
    }

    @Override
    public List<ItemBooking> listBookings() {
        return jdbcTemplate.query("SELECT * FROM item_booking ORDER BY id", ITEM_BOOKING_ROW_MAPPER);
    }

    @Override
    public List<ItemBooking> listBookingsByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_booking WHERE item_id = ? ORDER BY id", ITEM_BOOKING_ROW_MAPPER, itemId);
    }

    @Override
    public List<ItemBooking> listBookingsByGuestId(final int guestId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_booking WHERE guest_id = ? ORDER BY created_at DESC, id DESC",
                ITEM_BOOKING_ROW_MAPPER,
                guestId);
    }

    @Override
    public List<ItemBooking> listBookingsByOwnerId(final int ownerId) {
        return jdbcTemplate.query(
                "SELECT b.*"
                        + " FROM item_booking b"
                        + " JOIN item i ON i.id = b.item_id"
                        + " WHERE i.owner_id = ?"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ITEM_BOOKING_ROW_MAPPER,
                ownerId);
    }

    @Override
    public List<ItemBooking> listPendingBookingsByOwnerId(final int ownerId) {
        return jdbcTemplate.query(
                "SELECT b.*"
                        + " FROM item_booking b"
                        + " JOIN item i ON i.id = b.item_id"
                        + " WHERE i.owner_id = ? AND b.state = 'BOOKING_PENDING'"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ITEM_BOOKING_ROW_MAPPER,
                ownerId);
    }

    @Override
    public List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(final int ownerId) {
        return jdbcTemplate.query(
                "SELECT b.*"
                        + " FROM item_booking b"
                        + " JOIN item i ON i.id = b.item_id"
                        + " WHERE i.owner_id = ? AND b.state = 'BOOKING_PAYMENT_SUBMITTED'"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ITEM_BOOKING_ROW_MAPPER,
                ownerId);
    }

    @Override
    public List<ItemBooking> listActiveBookingsByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT b.*"
                        + " FROM item_booking b"
                        + " INNER JOIN item i ON i.id = b.item_id"
                        + " WHERE b.item_id = ?"
                        + " AND b.state IN (" + EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> i.owner_id)"
                        + " ORDER BY b.created_at DESC, b.id DESC",
                ITEM_BOOKING_ROW_MAPPER,
                itemId);
    }

    @Override
    public Optional<ItemBooking> findBookingByHostDecisionToken(final String hostDecisionToken) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_booking WHERE host_decision_token = ?",
                        ITEM_BOOKING_ROW_MAPPER,
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
                ITEM_BOOKING_ROW_MAPPER,
                hostDecisionTokens.toArray());
    }

    @Override
    public Optional<ItemBooking> findBookingById(final int bookingId) {
        return jdbcTemplate
                .query("SELECT * FROM item_booking WHERE id = ?", ITEM_BOOKING_ROW_MAPPER, bookingId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(final int bookingId, final int guestId) {
        return jdbcTemplate
                .query(
                        "SELECT v.*"
                                + " FROM item_booking b"
                                + " JOIN item_publication_version v ON v.id = b.item_version_id"
                                + " WHERE b.id = ? AND b.guest_id = ?",
                        ITEM_SNAPSHOT_ROW_MAPPER,
                        bookingId,
                        guestId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(final int bookingId, final int ownerId) {
        return jdbcTemplate
                .query(
                        "SELECT v.*"
                                + " FROM item_booking b"
                                + " JOIN item_publication_version v ON v.id = b.item_version_id"
                                + " WHERE b.id = ? AND v.owner_id = ?",
                        ITEM_SNAPSHOT_ROW_MAPPER,
                        bookingId,
                        ownerId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(
            final int versionId, final int itemId, final int guestId) {
        return jdbcTemplate
                .query(
                        "SELECT v.*"
                                + " FROM item_publication_version v"
                                + " JOIN item_booking b ON b.item_version_id = v.id"
                                + " WHERE v.id = ? AND v.item_id = ? AND b.guest_id = ?"
                                + " LIMIT 1",
                        ITEM_SNAPSHOT_ROW_MAPPER,
                        versionId,
                        itemId,
                        guestId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(
            final int versionId, final int itemId, final int ownerId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_publication_version WHERE id = ? AND item_id = ? AND owner_id = ?",
                        ITEM_SNAPSHOT_ROW_MAPPER,
                        versionId,
                        itemId,
                        ownerId)
                .stream()
                .findAny();
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForGuest(final int itemId, final int guestId) {
        return jdbcTemplate.query(
                "SELECT DISTINCT v.*"
                        + " FROM item_publication_version v"
                        + " JOIN item_booking b ON b.item_version_id = v.id"
                        + " WHERE v.item_id = ? AND b.guest_id = ?"
                        + " ORDER BY v.created_at DESC, v.id DESC",
                ITEM_SNAPSHOT_ROW_MAPPER,
                itemId,
                guestId);
    }

    @Override
    public List<ItemSnapshot> listSnapshotsByItemIdForOwner(final int itemId, final int ownerId) {
        return jdbcTemplate.query(
                "SELECT *"
                        + " FROM item_publication_version"
                        + " WHERE item_id = ? AND owner_id = ?"
                        + " ORDER BY created_at DESC, id DESC",
                ITEM_SNAPSHOT_ROW_MAPPER,
                itemId,
                ownerId);
    }

    @Override
    public boolean snapshotBookingsForPublicationEdit(final int itemId) {
        final Integer bookingsWithoutVersion = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item_booking"
                        + " WHERE item_id = ?"
                        + " AND item_version_id IS NULL"
                        + " AND state IN (" + PUBLICATION_EDIT_SNAPSHOT_STATES + ")",
                Integer.class,
                itemId);
        if (bookingsWithoutVersion == null || bookingsWithoutVersion == 0) {
            return true;
        }
        final int versionId = createCurrentPublicationVersion(itemId);
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET item_version_id = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE item_id = ?"
                        + " AND item_version_id IS NULL"
                        + " AND state IN (" + PUBLICATION_EDIT_SNAPSHOT_STATES + ")",
                versionId,
                itemId);
        if (updatedRows != bookingsWithoutVersion) {
            throw new IllegalStateException("Could not attach all active bookings to publication version " + versionId);
        }
        return true;
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
            final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName("item_booking")
                    .usingGeneratedKeyColumns("id");
            final Map<String, Object> args = new HashMap<>();
            args.put("item_id", itemId);
            args.put("guest_id", guestId);
            args.put("start_time", Timestamp.from(startTime.toInstant()));
            args.put("end_time", Timestamp.from(endTime.toInstant()));
            args.put("state", BookingState.BOOKING_PENDING.name());
            args.put("request_message", requestMessage);
            args.put("host_decision_token", hostDecisionToken);
            args.put("created_at", Timestamp.from(Instant.now()));
            args.put("updated_at", Timestamp.from(Instant.now()));
            id = insert.executeAndReturnKey(args).intValue();
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
            final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName("item_booking")
                    .usingGeneratedKeyColumns("id");
            final Map<String, Object> args = new HashMap<>();
            args.put("item_id", itemId);
            args.put("guest_id", ownerId);
            args.put("start_time", Timestamp.from(startTime.toInstant()));
            args.put("end_time", Timestamp.from(endTime.toInstant()));
            args.put("state", BookingState.BOOKING_CONFIRMED.name());
            args.put("request_message", null);
            args.put("host_decision_token", hostDecisionToken);
            args.put("host_decision_used_at", Timestamp.from(hostDecisionRecordedAt.toInstant()));
            args.put("created_at", Timestamp.from(Instant.now()));
            args.put("updated_at", Timestamp.from(Instant.now()));
            id = insert.executeAndReturnKey(args).intValue();
        }
        return findBookingById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted personal block booking " + id));
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
    public List<User> findUsersByIds(final Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        final String placeholders = String.join(", ", Collections.nCopies(userIds.size(), "?"));
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE id IN (" + placeholders + ")", USER_ROW_MAPPER, userIds.toArray());
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
                        BOOKING_PAYMENT_PROOF_ROW_MAPPER,
                        bookingId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<BookingPaymentProof> findPaymentProofById(final int proofId) {
        return jdbcTemplate
                .query("SELECT * FROM booking_payment_proof WHERE id = ?", BOOKING_PAYMENT_PROOF_ROW_MAPPER, proofId)
                .stream()
                .findAny();
    }

    @Override
    public boolean markBookingPaymentSubmitted(final int bookingId, final int guestId) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET state = 'BOOKING_PAYMENT_SUBMITTED', updated_at = CURRENT_TIMESTAMP"
                        + " WHERE id = ? AND guest_id = ? AND state = 'BOOKING_CONFIRMED'",
                bookingId,
                guestId);
        return updatedRows > 0;
    }

    @Override
    public boolean deletePaymentProofByBookingId(final int bookingId) {
        return jdbcTemplate.update("DELETE FROM booking_payment_proof WHERE booking_id = ?", bookingId) > 0;
    }

    @Override
    public boolean markBookingPaymentResubmitted(final int bookingId, final int guestId) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET state = 'BOOKING_PAYMENT_SUBMITTED', updated_at = CURRENT_TIMESTAMP"
                        + " WHERE id = ? AND guest_id = ? AND state = 'BOOKING_PAYMENT_REFUSED'",
                bookingId,
                guestId);
        return updatedRows > 0;
    }

    @Override
    public boolean markBookingPaymentRefused(final int bookingId, final int ownerId, final String reason) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking b"
                        + " SET state = 'BOOKING_PAYMENT_REFUSED', updated_at = CURRENT_TIMESTAMP"
                        + " FROM item i"
                        + " WHERE b.item_id = i.id"
                        + " AND b.id = ?"
                        + " AND i.owner_id = ?"
                        + " AND b.state = 'BOOKING_PAYMENT_SUBMITTED'",
                bookingId,
                ownerId);
        if (updatedRows == 0) {
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
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking b"
                        + " SET state = 'BOOKING_PAID', updated_at = CURRENT_TIMESTAMP"
                        + " FROM item i"
                        + " WHERE b.item_id = i.id"
                        + " AND b.id = ?"
                        + " AND i.owner_id = ?"
                        + " AND b.state = 'BOOKING_PAYMENT_SUBMITTED'",
                bookingId,
                ownerId);
        return updatedRows > 0;
    }

    @Override
    public Optional<Review> createReview(
            final int bookingId,
            final int reviewerUserId,
            final int revieweeUserId,
            final ReviewTargetType targetType,
            final int targetId,
            final int rating,
            final String comment) {
        if (postgresDialect) {
            final Integer id = jdbcTemplate.queryForObject(
                    "INSERT INTO review"
                            + " (booking_id, reviewer_user_id, reviewee_user_id, target_type, target_id, rating, comment)"
                            + " VALUES (?, ?, ?, CAST(? AS review_target_type), ?, ?, ?)"
                            + " ON CONFLICT (booking_id, reviewer_user_id, target_type) DO NOTHING"
                            + " RETURNING id",
                    Integer.class,
                    bookingId,
                    reviewerUserId,
                    revieweeUserId,
                    targetType.name(),
                    targetId,
                    rating,
                    comment);
            if (id == null) {
                return Optional.empty();
            }
            return findReviewById(id);
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
        args.put("comment", comment);
        try {
            final Number id = insert.executeAndReturnKey(args);
            return findReviewById(id.intValue());
        } catch (final DataIntegrityViolationException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Review> findReviewByBookingReviewerAndTargetType(
            final int bookingId, final int reviewerUserId, final ReviewTargetType targetType) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM review WHERE booking_id = ? AND reviewer_user_id = ? AND CAST(target_type AS VARCHAR(16)) = ?",
                        REVIEW_ROW_MAPPER,
                        bookingId,
                        reviewerUserId,
                        targetType.name())
                .stream()
                .findAny();
    }

    @Override
    public List<Review> listReviewsByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE CAST(target_type AS VARCHAR(16)) = ? AND target_id = ? ORDER BY created_at DESC, id DESC",
                REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId);
    }

    @Override
    public List<Review> listLatestReviewsByTarget(
            final ReviewTargetType targetType, final int targetId, final int limit) {
        final int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE CAST(target_type AS VARCHAR(16)) = ? AND target_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                REVIEW_ROW_MAPPER,
                targetType.name(),
                targetId,
                safeLimit);
    }

    @Override
    public List<Review> listReviewsByReviewer(final int reviewerUserId) {
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE reviewer_user_id = ? ORDER BY created_at DESC, id DESC",
                REVIEW_ROW_MAPPER,
                reviewerUserId);
    }

    @Override
    public List<Review> listReviewsByReviewee(final int revieweeUserId) {
        return jdbcTemplate.query(
                "SELECT * FROM review WHERE reviewee_user_id = ? ORDER BY created_at DESC, id DESC",
                REVIEW_ROW_MAPPER,
                revieweeUserId);
    }

    @Override
    public Optional<Review> findReviewById(final int reviewId) {
        return jdbcTemplate.query("SELECT * FROM review WHERE id = ?", REVIEW_ROW_MAPPER, reviewId).stream()
                .findAny();
    }

    @Override
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        final int deletedRows = jdbcTemplate.update(
                "DELETE FROM review WHERE id = ? AND reviewer_user_id = ?", reviewId, reviewerUserId);
        return deletedRows > 0;
    }

    @Override
    public RatingSummary ratingSummaryByTarget(final ReviewTargetType targetType, final int targetId) {
        return jdbcTemplate.query(
                "SELECT COALESCE(AVG(rating), 0) AS avg_rating, COUNT(*) AS total_reviews"
                        + " FROM review WHERE CAST(target_type AS VARCHAR(16)) = ? AND target_id = ?",
                rs -> {
                    if (!rs.next()) {
                        return RatingSummary.empty();
                    }
                    return new RatingSummary(rs.getDouble("avg_rating"), rs.getInt("total_reviews"));
                },
                targetType.name(),
                targetId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_availability WHERE item_id = ? ORDER BY start_time ASC LIMIT 1",
                        ITEM_AVAILABILITY_ROW_MAPPER,
                        itemId)
                .stream()
                .findAny();
    }

    @Override
    public boolean setItemActive(final int itemId, final boolean active) {
        final int updatedRows = jdbcTemplate.update("UPDATE item SET active = ? WHERE id = ?", active, itemId);
        return updatedRows > 0;
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item SET active = ? WHERE id = ? AND owner_id = ?", active, itemId, ownerId);
        return updatedRows > 0;
    }

    @Override
    public Optional<byte[]> findImageById(final int id) {
        if (!hasTable("item_media")) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                "SELECT image_data FROM item_media WHERE id = ?",
                rs -> {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getBytes("image_data"));
                    }
                    return Optional.<byte[]>empty();
                },
                id);
    }

    @Override
    public List<Integer> listImageIdsByItemIdOrdered(final int itemId) {
        if (!hasTable("item_media")) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT id FROM item_media WHERE item_id = ? ORDER BY display_order ASC", Integer.class, itemId);
    }

    @Override
    public Optional<Integer> findCoverImageIdByItemId(final int itemId) {
        if (!hasTable("item_media")) {
            return Optional.empty();
        }
        return jdbcTemplate
                .queryForList(
                        "SELECT id FROM item_media WHERE item_id = ? ORDER BY display_order ASC LIMIT 1",
                        Integer.class,
                        itemId)
                .stream()
                .findFirst();
    }

    @Override
    public int countImagesByItemId(final int itemId) {
        if (!hasTable("item_media")) {
            return 0;
        }
        final Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_media WHERE item_id = ?", Integer.class, itemId);
        return count == null ? 0 : count;
    }

    private static String marketplaceWhereClause(final ItemSearchCriteria criteria, final List<Object> args) {
        final StringBuilder sql = new StringBuilder(" WHERE i.active = TRUE");
        if (criteria == null) {
            return sql.toString();
        }
        if (criteria.getLocationOptionId() != null) {
            sql.append(" AND i.location_option_id = ?");
            args.add(criteria.getLocationOptionId());
        }
        if (criteria.getCapacity() != null) {
            sql.append(" AND i.capacity_people >= ?");
            args.add(criteria.getCapacity());
        }
        if (criteria.getMaxWeightKg() != null) {
            sql.append(" AND i.max_weight_kg >= ?");
            args.add(criteria.getMaxWeightKg());
        }
        if (criteria.getSearchQuery() != null) {
            sql.append(" AND i.title ILIKE ? ESCAPE '!'");
            args.add(setupSearchQuery(criteria.getSearchQuery()));
        }
        if (criteria.getDifficultyLevel() != null) {
            sql.append(" AND i.difficulty_level = ?");
            args.add(criteria.getDifficultyLevel());
        }
        if (criteria.getMinAverageRating() != null) {
            sql.append(" AND COALESCE((SELECT AVG(r.rating)"
                    + " FROM review r"
                    + " WHERE CAST(r.target_type AS VARCHAR(16)) = ?"
                    + " AND r.target_id = i.id), 0) >= ?");
            args.add(ReviewTargetType.ITEM.name());
            args.add(criteria.getMinAverageRating());
        }
        return sql.toString();
    }

    private static String setupSearchQuery(final String searchQuery) {
        String queryWithWildcards = searchQuery
                .trim()
                .replace("!", "!!") // Escape the escape character
                .replace("%", "!%")
                .replace("_", "!_") // Escape special characters
                .replaceAll("\\s+", "%"); // Replace whitespaces with wildcards

        return "%" + queryWithWildcards + "%";
    }

    private static String marketplaceOrderBy(final ItemSearchCriteria criteria) {
        final ItemSearchSort sort = criteria == null ? null : criteria.getSort();
        if (sort == null) {
            return " ORDER BY i.created_at DESC, i.id DESC";
        }
        return switch (sort) {
            case OLDEST -> " ORDER BY i.created_at ASC, i.id ASC";
            case PRICE_ASC -> " ORDER BY i.price_per_hour ASC, i.id ASC";
            case PRICE_DESC -> " ORDER BY i.price_per_hour DESC, i.id ASC";
            case NEWEST -> " ORDER BY i.created_at DESC, i.id DESC";
        };
    }

    @Override
    public Integer insertItem(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item").usingGeneratedKeyColumns("id");
        final Map<String, Object> args = new HashMap<>();
        args.put("owner_id", ownerId);
        args.put("type_id", typeId);
        args.put("title", title);
        args.put("description", description);
        args.put("price_per_hour", pricePerHour);
        args.put("capacity_people", capacityPeople);
        args.put("max_weight_kg", maxWeightKg);
        args.put("difficulty_level", difficultyLevel);
        args.put("location_option_id", locationOptionId);
        return insert.executeAndReturnKey(args).intValue();
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO item_availability (item_id, weekday, start_time, end_time) VALUES (?, ?::availability_weekday, ?, ?) RETURNING id",
                Integer.class,
                itemId,
                weekday.name(),
                Time.valueOf(startTime),
                Time.valueOf(endTime));
    }

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData, final int displayOrder) {
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item_media").usingGeneratedKeyColumns("id");
        final Map<String, Object> args = new HashMap<>();
        args.put("item_id", itemId);
        args.put("image_data", imageData);
        args.put("display_order", displayOrder);
        return insert.executeAndReturnKey(args).intValue();
    }

    @Override
    public boolean deleteImage(final int itemId, final int imageId) {
        final List<Integer> currentOrder = jdbcTemplate.queryForList(
                "SELECT id FROM item_media WHERE item_id = ? ORDER BY display_order ASC", Integer.class, itemId);
        if (!currentOrder.contains(imageId)) {
            return false;
        }
        // Park surviving rows out of the unique-constraint range, delete the target,
        // then re-pack 0..N-1 deterministically.
        jdbcTemplate.update("UPDATE item_media SET display_order = -1 - display_order WHERE item_id = ?", itemId);
        final int deleted = jdbcTemplate.update("DELETE FROM item_media WHERE id = ? AND item_id = ?", imageId, itemId);
        if (deleted == 0) {
            jdbcTemplate.update("UPDATE item_media SET display_order = -1 - display_order WHERE item_id = ?", itemId);
            return false;
        }
        int newPosition = 0;
        for (final Integer survivingId : currentOrder) {
            if (survivingId.intValue() == imageId) {
                continue;
            }
            jdbcTemplate.update(
                    "UPDATE item_media SET display_order = ? WHERE id = ? AND item_id = ?",
                    newPosition,
                    survivingId,
                    itemId);
            newPosition++;
        }
        return true;
    }

    @Override
    public void reorderImages(final int itemId, final List<Integer> imageIdsInOrder) {
        if (imageIdsInOrder == null || imageIdsInOrder.isEmpty()) {
            return;
        }
        jdbcTemplate.update("UPDATE item_media SET display_order = -1 - display_order WHERE item_id = ?", itemId);
        for (int position = 0; position < imageIdsInOrder.size(); position++) {
            jdbcTemplate.update(
                    "UPDATE item_media SET display_order = ? WHERE id = ? AND item_id = ?",
                    position,
                    imageIdsInOrder.get(position),
                    itemId);
        }
    }

    @Override
    public Integer replacePrimaryImage(final int itemId, final byte[] imageData) {
        if (!hasTable("item_media")) {
            return null;
        }
        final Integer existingImageId = jdbcTemplate
                .queryForList(
                        "SELECT id FROM item_media WHERE item_id = ? ORDER BY display_order ASC, id ASC",
                        Integer.class,
                        itemId)
                .stream()
                .findFirst()
                .orElse(null);
        if (existingImageId == null) {
            return insertImage(itemId, imageData, 0);
        }
        final int updatedRows =
                jdbcTemplate.update("UPDATE item_media SET image_data = ? WHERE id = ?", imageData, existingImageId);
        return updatedRows > 0 ? existingImageId : null;
    }

    @Override
    public Integer replacePrimaryImageForOwner(final int itemId, final int ownerId, final byte[] imageData) {
        if (findItemByIdForOwner(itemId, ownerId).isEmpty()) {
            return null;
        }
        return replacePrimaryImage(itemId, imageData);
    }

    private int createCurrentPublicationVersion(final int itemId) {
        final Map<String, Object> itemData = jdbcTemplate.queryForMap(
                "SELECT i.id AS item_id, i.owner_id, i.type_id, i.title, i.description, i.price_per_hour,"
                        + " i.capacity_people, i.max_weight_kg, i.difficulty_level, i.location_option_id,"
                        + " lo.name AS location_name,"
                        + " (SELECT m.image_data FROM item_media m WHERE m.item_id = i.id"
                        + " ORDER BY m.display_order ASC, m.id ASC LIMIT 1) AS cover_image_data"
                        + " FROM item i"
                        + " JOIN location_option lo ON lo.id = i.location_option_id"
                        + " WHERE i.id = ?",
                itemId);
        itemData.put("created_at", Timestamp.from(Instant.now()));
        final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("item_publication_version")
                .usingColumns(
                        "item_id",
                        "owner_id",
                        "type_id",
                        "title",
                        "description",
                        "price_per_hour",
                        "capacity_people",
                        "max_weight_kg",
                        "difficulty_level",
                        "location_option_id",
                        "location_name",
                        "cover_image_data",
                        "created_at")
                .usingGeneratedKeyColumns("id");
        return insert.executeAndReturnKey(itemData).intValue();
    }

    private boolean hasBookings(final int itemId) {
        final Integer bookingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_booking WHERE item_id = ?", Integer.class, itemId);
        return bookingCount != null && bookingCount > 0;
    }

    private boolean hasFutureRetainedBookings(final int itemId) {
        final Integer bookingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item_booking"
                        + " WHERE item_id = ?"
                        + " AND state NOT IN ('BOOKING_REJECTED', 'BOOKING_CANCELLED')"
                        + " AND end_time > CURRENT_TIMESTAMP",
                Integer.class,
                itemId);
        return bookingCount != null && bookingCount > 0;
    }

    private boolean hasTable(final String tableName) {
        final Boolean hasTable = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
                return tables.next();
            }
        });
        return Boolean.TRUE.equals(hasTable);
    }

    private static Integer readRequiredIntColumn(final ResultSet rs, final String column) throws SQLException {
        final int value = rs.getInt(column);
        if (rs.wasNull()) {
            throw new IllegalStateException("Expected non-null integer column: " + column);
        }
        return value;
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
