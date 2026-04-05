package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.CatalogUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemCatalogJdbcDao implements ItemCatalogDao {
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

    private static final @NonNull RowMapper<CatalogUser> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final CatalogUser user = new CatalogUser();
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
                final OffsetDateTime startTime = readOffsetDateTime(rs, "start_time");
                final OffsetDateTime endTime = readOffsetDateTime(rs, "end_time");
                final ItemAvailability availability = new ItemAvailability();
                availability.setId(rs.getInt("id"));
                availability.setItemId(rs.getInt("item_id"));
                availability.setWeekday(resolveWeekday(startTime, endTime));
                availability.setStartTime(formatTime(startTime));
                availability.setEndTime(formatTime(endTime));
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
    public ItemCatalogJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Item> listItems() {
        return jdbcTemplate.query("SELECT * FROM item ORDER BY id", ITEM_ROW_MAPPER);
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return jdbcTemplate.query("SELECT * FROM item WHERE id = ?", ITEM_ROW_MAPPER, id).stream().findAny();
    }

    @Override
    public Optional<CatalogUser> findUserById(final int id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream().findAny();
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return jdbcTemplate.query("SELECT * FROM item_type WHERE id = ?", ITEM_TYPE_ROW_MAPPER, id).stream().findAny();
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
        return jdbcTemplate.query("SELECT * FROM item_booking WHERE item_id = ? ORDER BY id", ITEM_BOOKING_ROW_MAPPER, itemId);
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
    public Optional<String> findImageUrlByItemId(final int itemId) {
        if (!hasTable("item_media")) {
            return Optional.empty();
        }
        return jdbcTemplate.query("SELECT image_url FROM item_media WHERE item_id = ?", rs -> {
            if (rs.next()) {
                return Optional.ofNullable(rs.getString("image_url"));
            }
            return Optional.<String>empty();
        }, itemId);
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

    private static String formatTime(final OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime().format(TIME_FORMAT);
    }

    private static String resolveWeekday(final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime != null) {
            return startTime.getDayOfWeek().name();
        }
        if (endTime != null) {
            return endTime.getDayOfWeek().name();
        }
        return null;
    }
}
