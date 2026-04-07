package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        item.setLocation(rs.getString("location"));
        item.setActive(rs.getBoolean("active"));
        item.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
        return item;
    };

    private static final @NonNull RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final User user = new User();
        user.setId(rs.getInt("id"));
        user.setCreatedAt(formatDateTime(readOffsetDateTime(rs, "created_at")));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
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
        booking.setStartTime(formatDateTime(readOffsetDateTime(rs, "start_time")));
        booking.setEndTime(formatDateTime(readOffsetDateTime(rs, "end_time")));
        return booking;
    };

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ItemJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Item> listItems() {
        return jdbcTemplate.query("SELECT * FROM item ORDER BY id", ITEM_ROW_MAPPER);
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return jdbcTemplate.query("SELECT * FROM item WHERE id = ?", ITEM_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<User> findUserById(final int id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return jdbcTemplate.query("SELECT * FROM item_type WHERE id = ?", ITEM_TYPE_ROW_MAPPER, id).stream()
                .findAny();
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
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value.toString());
    }

    private static String formatDateTime(final OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
