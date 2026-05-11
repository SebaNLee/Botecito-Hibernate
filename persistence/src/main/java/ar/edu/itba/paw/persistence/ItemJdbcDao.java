package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemJdbcDao implements ItemDao {

    private static final String ITEM_SELECT = "SELECT i.id, i.host_id, v.type_id, v.title, v.description,"
            + " v.price, v.capacity, v.weight, v.difficulty,"
            + " v.location_id, lo.name AS location,"
            + " i.status, i.created_at"
            + " FROM item i"
            + " JOIN version v ON v.id = (SELECT MAX(v2.id) FROM version v2 WHERE v2.item_id = i.id)"
            + " JOIN location lo ON lo.id = v.location_id";

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ItemJdbcDao(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public List<LocationOption> listLocationOptions() {
        return jdbcTemplate.query("SELECT id, name FROM location ORDER BY id", (rs, rowNum) -> {
            final LocationOption locationOption = new LocationOption();
            locationOption.setId(rs.getInt("id"));
            locationOption.setName(rs.getString("name"));
            return locationOption;
        });
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return jdbcTemplate
                .query(ITEM_SELECT + " WHERE i.id = ? AND i.status = 'ACTIVE'", ItemJdbcRowMappers.ITEM_ROW_MAPPER, id)
                .stream()
                .findAny();
    }

    @Override
    public Optional<Item> findItemByIdForOwner(final int id, final int ownerId) {
        return jdbcTemplate
                .query(
                        ITEM_SELECT + " WHERE i.id = ? AND i.host_id = ?",
                        ItemJdbcRowMappers.ITEM_ROW_MAPPER,
                        id,
                        ownerId)
                .stream()
                .findAny();
    }

    @Override
    public Optional<Item> findAnyItemById(final int id) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE i.id = ?", ItemJdbcRowMappers.ITEM_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return jdbcTemplate
                .query("SELECT * FROM item_type WHERE id = ?", ItemJdbcRowMappers.ITEM_TYPE_ROW_MAPPER, id)
                .stream()
                .findAny();
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
        return updatePublicationVersion(
                itemId, ownerId, title, description, pricePerHour, difficultyLevel, locationOptionId);
    }

    @Override
    // TODO versioning reference
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + ItemPersistenceSql.ITEM_BOOKING_VERSION_JOIN
                        + " AND b.version_id = (SELECT MAX(v2.id) FROM version v2 WHERE v2.item_id = i.id)"
                        + " AND b.status IN (" + ItemPersistenceSql.EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> i.host_id)",
                Integer.class,
                itemId);
        return count != null && count > 0;
    }

    @Override
    public boolean deleteItemById(final int itemId) {
        return deleteItemWithOwnershipScope(itemId, null, this::findAnyItemById);
    }

    @Override
    public boolean deleteItemByIdForOwner(final int itemId, final int ownerId) {
        return deleteItemWithOwnershipScope(itemId, ownerId, id -> findItemByIdForOwner(id, ownerId));
    }

    // TODO versioning reference
    private boolean deleteItemWithOwnershipScope(
            final int itemId, final Integer ownerId, final IntFunction<Optional<Item>> findItem) {
        final Optional<Item> item = findItem.apply(itemId);
        if (item.isEmpty()) {
            return false;
        }
        if (hasBookingsBlockingHardDelete(itemId)) {
            if (Boolean.TRUE.equals(item.get().getActive())) {
                if (!snapshotBookingsForPublicationEdit(itemId)) {
                    return false;
                }
                return updateCurrentVersionActive(itemId, ownerId, false) > 0;
            }
            return false;
        }
        return jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId) > 0;
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
        final Timestamp now = Timestamp.from(Instant.now());
        final int itemId = insertItemRow(ownerId, now, "Could not create item for owner " + ownerId);
        try {
            insertPublicationVersion(buildPublicationVersionData(
                    itemId,
                    typeId,
                    title,
                    description,
                    pricePerHour,
                    capacityPeople,
                    maxWeightKg,
                    difficultyLevel,
                    locationOptionId,
                    now));
            return findAnyItemById(itemId)
                    .orElseThrow(() -> new IllegalStateException("Could not read inserted item " + itemId));
        } catch (final RuntimeException exception) {
            jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);
            throw exception;
        }
    }

    @Override
    // TODO versioning reference
    public boolean snapshotBookingsForPublicationEdit(final int itemId) {
        // Since item_version_id is now NOT NULL, bookings are always tied to a publication version.
        return true;
    }

    @Override
    public boolean setItemActive(final int itemId, final boolean active) {
        return updateCurrentVersionActive(itemId, null, active) > 0;
    }

    @Override
    public boolean setItemActiveForOwner(final int itemId, final int ownerId, final boolean active) {
        return updateCurrentVersionActive(itemId, ownerId, active) > 0;
    }

    // TODO versioning reference
    private int insertPublicationVersion(final @NonNull Map<String, Object> itemData) {
        final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("version")
                .usingColumns(
                        "item_id",
                        "type_id",
                        "title",
                        "description",
                        "price",
                        "capacity",
                        "weight",
                        "difficulty",
                        "location_id",
                        "timezone",
                        "created_at")
                .usingGeneratedKeyColumns("id");
        return insert.executeAndReturnKey(itemData).intValue();
    }

    private int insertItemRow(final int ownerId, final Timestamp itemCreatedAt, final String failureMessage) {
        if (postgresDialect) {
            return Objects.requireNonNull(
                    jdbcTemplate.queryForObject(
                            "INSERT INTO item (host_id, status, created_at) VALUES (?, 'ACTIVE', ?) RETURNING id",
                            Integer.class,
                            ownerId,
                            itemCreatedAt),
                    failureMessage);
        }
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item").usingGeneratedKeyColumns("id");
        final Map<String, Object> itemData = new HashMap<>();
        itemData.put("host_id", ownerId);
        itemData.put("status", "ACTIVE");
        itemData.put("created_at", itemCreatedAt);
        return insert.executeAndReturnKey(itemData).intValue();
    }

    // TODO versioning reference
    private @NonNull Map<String, Object> buildPublicationVersionData(
            final int itemId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final Timestamp versionCreatedAt) {
        final Map<String, Object> itemData = new HashMap<>();
        itemData.put("item_id", itemId);
        itemData.put("type_id", typeId);
        itemData.put("title", title);
        itemData.put("description", description);
        itemData.put("price", pricePerHour);
        itemData.put("capacity", capacityPeople);
        itemData.put("weight", maxWeightKg == null ? 2000 : maxWeightKg.intValue());
        final int difficulty = difficultyLevel != null ? difficultyLevel.intValue() : 1;
        itemData.put("difficulty", difficulty);
        itemData.put("location_id", locationOptionId);
        itemData.put("timezone", "UTC");
        itemData.put("created_at", versionCreatedAt);
        return itemData;
    }

    // TODO versioning reference
    private boolean updatePublicationVersion(
            final int itemId,
            final Integer ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        final Optional<Map<String, Object>> currentVersionData = findCurrentPublicationVersionData(itemId, ownerId);
        if (currentVersionData.isEmpty()) {
            return false;
        }
        final Map<String, Object> data = currentVersionData.get();
        final int currentVersionId =
                readRequiredNumber(data, "current_version_id").intValue();
        if (!currentVersionHasBookingReferences(currentVersionId)) {
            return updateCurrentPublicationVersion(
                    currentVersionId,
                    ownerId,
                    title,
                    description,
                    pricePerHour,
                    difficultyLevel,
                    locationOptionId,
                    itemId);
        }

        insertPublicationVersion(buildPublicationVersionData(
                itemId,
                readRequiredNumber(data, "type_id").intValue(),
                title,
                description,
                pricePerHour,
                readRequiredNumber(data, "capacity").intValue(),
                java.math.BigDecimal.valueOf(readRequiredNumber(data, "weight").intValue()),
                difficultyLevel,
                locationOptionId,
                Timestamp.from(Instant.now())));
        return true;
    }

    // TODO versioning reference
    private boolean updateCurrentPublicationVersion(
            final int currentVersionId,
            final Integer ownerId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId,
            final int itemId) {
        final List<Object> args = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("UPDATE version"
                + " SET title = ?, description = ?, price = ?, difficulty = ?,"
                + " location_id = ?, created_at = CURRENT_TIMESTAMP"
                + " WHERE id = ?");
        args.add(title);
        args.add(description);
        args.add(pricePerHour);
        args.add(difficultyLevel);
        args.add(locationOptionId);
        args.add(currentVersionId);
        if (ownerId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM item i WHERE i.id = ? AND i.host_id = ?)");
            args.add(itemId);
            args.add(ownerId);
        }
        return jdbcTemplate.update(requireSql(sql), args.toArray()) > 0;
    }

    // TODO versioning reference
    private Optional<Map<String, Object>> findCurrentPublicationVersionData(final int itemId, final Integer ownerId) {
        final List<Object> args = new ArrayList<>();
        final StringBuilder sql =
                new StringBuilder("SELECT v.id AS current_version_id, i.host_id, i.created_at AS item_created_at,"
                        + " v.type_id, v.capacity, v.weight, i.status"
                        + " FROM item i"
                        + " JOIN version v ON v.id = (SELECT MAX(v2.id) FROM version v2 WHERE v2.item_id = i.id)"
                        + " WHERE i.id = ?");
        args.add(itemId);
        if (ownerId != null) {
            sql.append(" AND i.host_id = ?");
            args.add(ownerId);
        }
        return jdbcTemplate.queryForList(requireSql(sql), args.toArray()).stream()
                .findFirst();
    }

    // TODO versioning reference
    private boolean currentVersionHasBookingReferences(final int currentVersionId) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE version_id = ?", Integer.class, currentVersionId);
        return count != null && count > 0;
    }

    // TODO versioning reference
    private int updateCurrentVersionActive(final int itemId, final Integer ownerId, final boolean active) {
        final List<Object> args = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("UPDATE item SET status = ?");
        if (postgresDialect) {
            sql.append("::item_status_enum");
        }
        sql.append(" WHERE id = ?");
        args.add(active ? "ACTIVE" : "INACTIVE");
        args.add(itemId);
        if (ownerId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM item i WHERE i.id = ? AND i.host_id = ?)");
            args.add(itemId);
            args.add(ownerId);
        }
        return jdbcTemplate.update(requireSql(sql), args.toArray());
    }

    /**
     * True when the item still has guest (non-owner) bookings that are not cancelled/rejected and have not ended yet,
     * so it cannot be removed from the database; an active listing is only deactivated instead.
     */
    // TODO versioning reference
    private boolean hasBookingsBlockingHardDelete(final int itemId) {
        final Integer bookingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + ItemPersistenceSql.ITEM_BOOKING_VERSION_JOIN
                        + ItemPersistenceSql.ITEM_BOOKING_HARD_DELETE_BLOCKERS,
                Integer.class,
                itemId);
        return bookingCount != null && bookingCount > 0;
    }

    private static Number readRequiredNumber(final Map<String, Object> row, final String column) {
        final Object value = row.get(column);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Expected numeric column: " + column);
        }
        return number;
    }

    private static @NonNull String requireSql(final @NonNull StringBuilder sql) {
        final String value = sql.toString();
        if (value == null) {
            throw new IllegalStateException("Generated SQL cannot be null");
        }
        return value;
    }
}
