package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJdbcDao implements ItemDao {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ITEM_SELECT = "SELECT i.id, i.owner_id, i.type_id, i.title, i.description,"
            + " i.price_per_hour, i.capacity_people, i.max_weight_kg, i.difficulty_level,"
            + " i.location_option_id, lo.name AS location,"
            + " i.active, i.owner_delete_token, i.owner_delete_used_at, i.created_at"
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
        item.setOwnerDeleteUsedAt(formatDateTime(readOffsetDateTime(rs, "owner_delete_used_at")));
        item.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
        return item;
    };

    private static final @NonNull RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final User user = new User();
        user.setId(rs.getInt("id"));
        user.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
        user.setGivenName(rs.getString("given_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPreferredLanguage(rs.getString("preferred_language"));
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
                availability.setWeekday(rs.getString("weekday"));
                availability.setStartTime(rs.getTime("start_time").toLocalTime().format(TIME_FORMAT));
                availability.setEndTime(rs.getTime("end_time").toLocalTime().format(TIME_FORMAT));
                return availability;
            };

    private static final @NonNull RowMapper<ItemBooking> ITEM_BOOKING_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final ItemBooking booking = new ItemBooking();
        booking.setId(rs.getInt("id"));
        booking.setItemId(rs.getInt("item_id"));
        booking.setGuestId(rs.getInt("guest_id"));
        booking.setStartTime(formatDateTime(readOffsetDateTime(rs, "start_time")));
        booking.setEndTime(formatDateTime(readOffsetDateTime(rs, "end_time")));
        booking.setState(rs.getString("state"));
        booking.setRequestMessage(rs.getString("request_message"));
        booking.setHostDecisionToken(rs.getString("host_decision_token"));
        booking.setHostDecisionUsedAt(formatDateTime(readOffsetDateTime(rs, "host_decision_used_at")));
        booking.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
        booking.setUpdatedAt(formatDateTime(readOffsetDateTime(rs, "updated_at")));
        return booking;
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
                proof.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
                return proof;
            };

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Autowired
    public ItemJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
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
    public Optional<ItemBooking> findBookingById(final int bookingId) {
        return jdbcTemplate
                .query("SELECT * FROM item_booking WHERE id = ?", ITEM_BOOKING_ROW_MAPPER, bookingId)
                .stream()
                .findAny();
    }

    @Override
    public ItemBooking createBookingRequest(
            final int itemId,
            final int guestId,
            final OffsetDateTime startTime,
            final OffsetDateTime endTime,
            final String requestMessage,
            final String hostDecisionToken) {
        final int id = Objects.requireNonNull(
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
        return findBookingById(id)
                .orElseThrow(() -> new IllegalStateException("Could not read inserted booking " + id));
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
    public BookingPaymentProof createPaymentProof(
            final int bookingId,
            final int uploaderId,
            final String fileName,
            final String contentType,
            final byte[] fileData) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO booking_payment_proof"
                                + " (booking_id, uploader_id, file_name, content_type, file_data)"
                                + " VALUES (?, ?, ?, ?, ?)"
                                + " RETURNING id",
                        Integer.class,
                        bookingId,
                        uploaderId,
                        fileName,
                        contentType,
                        fileData),
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
    public List<Integer> listImageIdsByItemId(final int itemId) {
        if (!hasTable("item_media")) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList("SELECT id FROM item_media WHERE item_id = ?", Integer.class, itemId);
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
        final String sort = criteria == null ? null : criteria.getSort();
        if (sort == null) {
            return " ORDER BY i.price_per_hour ASC, i.id ASC";
        }
        return switch (sort) {
            case "titleAsc" -> " ORDER BY LOWER(i.title) ASC, i.id ASC";
            case "titleDesc" -> " ORDER BY LOWER(i.title) DESC, i.id ASC";
            case "priceDesc" -> " ORDER BY i.price_per_hour DESC, i.id ASC";
            default -> " ORDER BY i.price_per_hour ASC, i.id ASC";
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
    public Integer insertImage(final int itemId, final byte[] imageData) {
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item_media").usingGeneratedKeyColumns("id");
        final Map<String, Object> args = new HashMap<>();
        args.put("item_id", itemId);
        args.put("image_data", imageData);
        return insert.executeAndReturnKey(args).intValue();
    }

    private boolean hasTable(final String tableName) {
        final Boolean hasTable = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
                return tables.next();
            }
        });
        return Boolean.TRUE.equals(hasTable);
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

    private static String formatDateTime(final OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
