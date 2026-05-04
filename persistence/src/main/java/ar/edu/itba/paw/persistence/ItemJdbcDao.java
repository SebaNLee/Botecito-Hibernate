package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSearchSort;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.ReviewTargetType;
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

    private static final String ITEM_SELECT = "SELECT i.id, v.owner_id, v.type_id, v.title, v.description,"
            + " v.price_per_hour, v.capacity_people, v.max_weight_kg, v.difficulty_level,"
            + " v.location_option_id, lo.name AS location,"
            + " v.active, v.owner_delete_token, v.item_created_at AS created_at"
            + " FROM item i"
            + " JOIN item_publication_version v ON v.id = i.version_id"
            + " JOIN location_option lo ON lo.id = v.location_option_id";

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    @Autowired
    public ItemJdbcDao(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public List<Item> listItems() {
        return jdbcTemplate.query(
                ITEM_SELECT + " WHERE v.active = TRUE ORDER BY i.id", ItemJdbcRowMappers.ITEM_ROW_MAPPER);
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
        return jdbcTemplate.query(sql, ItemJdbcRowMappers.ITEM_ROW_MAPPER, args.toArray());
    }

    @Override
    public int countItems(final ItemSearchCriteria criteria) {
        final List<Object> args = new ArrayList<>();
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item i"
                        + " JOIN item_publication_version v ON v.id = i.version_id"
                        + " JOIN location_option lo ON lo.id = v.location_option_id"
                        + marketplaceWhereClause(criteria, args),
                Integer.class,
                args.toArray());
        return count == null ? 0 : count;
    }

    @Override
    public List<Item> listItemsByOwnerId(final int ownerId) {
        return jdbcTemplate.query(
                ITEM_SELECT + " WHERE v.owner_id = ? ORDER BY i.id DESC", ItemJdbcRowMappers.ITEM_ROW_MAPPER, ownerId);
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
        return jdbcTemplate
                .query(ITEM_SELECT + " WHERE i.id = ? AND v.active = TRUE", ItemJdbcRowMappers.ITEM_ROW_MAPPER, id)
                .stream()
                .findAny();
    }

    @Override
    public Optional<Item> findItemByIdForOwner(final int id, final int ownerId) {
        return jdbcTemplate
                .query(
                        ITEM_SELECT + " WHERE i.id = ? AND v.owner_id = ?",
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
    public boolean updatePublication(
            final int itemId,
            final String title,
            final String description,
            final int pricePerHour,
            final Integer difficultyLevel,
            final int locationOptionId) {
        return updatePublicationVersion(
                itemId, null, title, description, pricePerHour, difficultyLevel, locationOptionId);
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
    public boolean hasBlockingBookingsForEdition(final int itemId) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + ItemPersistenceSql.ITEM_BOOKING_VERSION_JOIN
                        + " AND b.item_version_id IS NULL"
                        + " AND b.state IN (" + ItemPersistenceSql.EDIT_CONFLICT_BOOKING_STATES + ")"
                        + " AND (b.guest_id IS NULL OR b.guest_id <> v.owner_id)",
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

    private boolean deleteItemWithOwnershipScope(
            final int itemId, final Integer ownerId, final IntFunction<Optional<Item>> findItem) {
        final Optional<Item> item = findItem.apply(itemId);
        if (item.isEmpty()) {
            return false;
        }
        if (hasBookingsBlockingHardDelete(itemId)) {
            if (Boolean.TRUE.equals(item.get().getActive())) {
                snapshotBookingsForPublicationEdit(itemId);
                return updateCurrentVersionActive(itemId, ownerId, false) > 0;
            }
            return false;
        }
        clearCurrentVersionOwnerDeleteToken(itemId);
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
        final Map<String, Object> versionData = buildPublicationVersionData(
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
                ownerDeleteToken,
                now,
                now,
                null);
        final int versionId = insertPublicationVersion(versionData);
        try {
            final int id = insertItemRow(versionId, "Could not create item for owner " + ownerId);
            return findAnyItemById(id)
                    .orElseThrow(() -> new IllegalStateException("Could not read inserted item " + id));
        } catch (final RuntimeException exception) {
            jdbcTemplate.update("DELETE FROM item_publication_version WHERE id = ?", versionId);
            throw exception;
        }
    }

    @Override
    public boolean snapshotBookingsForPublicationEdit(final int itemId) {
        final Integer bookingsWithoutVersion = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)"
                        + " FROM item_booking"
                        + " WHERE item_id = ?"
                        + " AND item_version_id IS NULL"
                        + " AND state IN (" + ItemPersistenceSql.PUBLICATION_EDIT_SNAPSHOT_STATES + ")",
                Integer.class,
                itemId);
        if (bookingsWithoutVersion == null || bookingsWithoutVersion == 0) {
            return true;
        }
        final Integer versionId = findCurrentVersionIdByItemId(itemId);
        if (versionId == null) {
            throw new IllegalStateException("Could not find current publication version for item " + itemId);
        }
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item_booking"
                        + " SET item_version_id = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE item_id = ?"
                        + " AND item_version_id IS NULL"
                        + " AND state IN (" + ItemPersistenceSql.PUBLICATION_EDIT_SNAPSHOT_STATES + ")",
                versionId,
                itemId);
        if (updatedRows != bookingsWithoutVersion) {
            throw new IllegalStateException("Could not attach all active bookings to publication version " + versionId);
        }
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
        final Timestamp now = Timestamp.from(Instant.now());
        final int versionId = insertPublicationVersion(buildPublicationVersionData(
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
                null,
                now,
                now,
                null));
        try {
            return insertItemRow(versionId, "Could not insert item for owner " + ownerId);
        } catch (final RuntimeException exception) {
            jdbcTemplate.update("DELETE FROM item_publication_version WHERE id = ?", versionId);
            throw exception;
        }
    }

    private static String marketplaceWhereClause(final ItemSearchCriteria criteria, final List<Object> args) {
        final StringBuilder sql = new StringBuilder(" WHERE v.active = TRUE");
        if (criteria == null) {
            return sql.toString();
        }
        if (criteria.getLocationOptionId() != null) {
            sql.append(" AND v.location_option_id = ?");
            args.add(criteria.getLocationOptionId());
        }
        if (criteria.getCapacity() != null) {
            sql.append(" AND v.capacity_people >= ?");
            args.add(criteria.getCapacity());
        }
        if (criteria.getMaxWeightKg() != null) {
            sql.append(" AND v.max_weight_kg >= ?");
            args.add(criteria.getMaxWeightKg());
        }
        if (criteria.getSearchQuery() != null) {
            sql.append(" AND v.title ILIKE ? ESCAPE '!'");
            args.add(setupSearchQuery(criteria.getSearchQuery()));
        }
        if (criteria.getDifficultyLevel() != null) {
            sql.append(" AND v.difficulty_level = ?");
            args.add(criteria.getDifficultyLevel());
        }
        if (criteria.getMinAverageRating() != null) {
            sql.append(" AND COALESCE((SELECT AVG(r.rating)"
                    + " FROM review r"
                    + " WHERE "
                    + ItemPersistenceSql.REVIEW_R_TARGET_TYPE_EQUALS
                    + " AND r.target_id = i.id), 0) >= ?");
            args.add(ReviewTargetType.ITEM.name());
            args.add(criteria.getMinAverageRating());
        }
        return sql.toString();
    }

    private static String setupSearchQuery(final String searchQuery) {
        String queryWithWildcards = searchQuery
                .trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replaceAll("\\s+", "%");

        return "%" + queryWithWildcards + "%";
    }

    private static String marketplaceOrderBy(final ItemSearchCriteria criteria) {
        final ItemSearchSort sort = criteria == null ? null : criteria.getSort();
        if (sort == null) {
            return " ORDER BY v.item_created_at DESC, i.id DESC";
        }
        return switch (sort) {
            case OLDEST -> " ORDER BY v.item_created_at ASC, i.id ASC";
            case PRICE_ASC -> " ORDER BY v.price_per_hour ASC, i.id ASC";
            case PRICE_DESC -> " ORDER BY v.price_per_hour DESC, i.id ASC";
            case NEWEST -> " ORDER BY v.item_created_at DESC, i.id DESC";
        };
    }

    private int insertPublicationVersion(final @NonNull Map<String, Object> itemData) {
        final SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("item_publication_version")
                .usingColumns(
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
                        "active",
                        "owner_delete_token",
                        "item_created_at",
                        "created_at")
                .usingGeneratedKeyColumns("id");
        return insert.executeAndReturnKey(itemData).intValue();
    }

    private int insertItemRow(final int versionId, final String failureMessage) {
        if (postgresDialect) {
            return Objects.requireNonNull(
                    jdbcTemplate.queryForObject(
                            "INSERT INTO item (version_id) VALUES (?) RETURNING id", Integer.class, versionId),
                    failureMessage);
        }
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item").usingGeneratedKeyColumns("id");
        final Map<String, Object> itemData = new HashMap<>();
        itemData.put("version_id", versionId);
        return insert.executeAndReturnKey(itemData).intValue();
    }

    private @NonNull Map<String, Object> buildPublicationVersionData(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final boolean active,
            final String ownerDeleteToken,
            final Timestamp itemCreatedAt,
            final Timestamp versionCreatedAt,
            final byte[] coverImageData) {
        final Map<String, Object> itemData = new HashMap<>();
        itemData.put("owner_id", ownerId);
        itemData.put("type_id", typeId);
        itemData.put("title", title);
        itemData.put("description", description);
        itemData.put("price_per_hour", pricePerHour);
        itemData.put("capacity_people", capacityPeople);
        itemData.put("max_weight_kg", maxWeightKg);
        itemData.put("difficulty_level", difficultyLevel);
        itemData.put("location_option_id", locationOptionId);
        itemData.put("location_name", findLocationNameById(locationOptionId));
        itemData.put("cover_image_data", coverImageData);
        itemData.put("active", active);
        itemData.put("owner_delete_token", ownerDeleteToken);
        itemData.put("item_created_at", itemCreatedAt);
        itemData.put("created_at", versionCreatedAt);
        return itemData;
    }

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

        final String ownerDeleteToken = (String) data.get("owner_delete_token");
        final int newVersionId = insertPublicationVersion(buildPublicationVersionData(
                readRequiredNumber(data, "owner_id").intValue(),
                readRequiredNumber(data, "type_id").intValue(),
                title,
                description,
                pricePerHour,
                readRequiredNumber(data, "capacity_people").intValue(),
                (BigDecimal) data.get("max_weight_kg"),
                difficultyLevel,
                locationOptionId,
                Boolean.TRUE.equals(data.get("active")),
                null,
                ItemJdbcRowMappers.toTimestamp(data.get("item_created_at")),
                Timestamp.from(Instant.now()),
                ItemPublicationCoverJdbcSupport.findCurrentCoverImageData(jdbcTemplate, itemId)));
        jdbcTemplate.update(
                "UPDATE item_publication_version SET owner_delete_token = NULL WHERE id = ?", currentVersionId);
        final int updatedRows = jdbcTemplate.update(
                "UPDATE item SET version_id = ? WHERE id = ? AND version_id = ?",
                newVersionId,
                itemId,
                currentVersionId);
        if (updatedRows == 0) {
            throw new IllegalStateException("Could not switch current publication version for item " + itemId);
        }
        if (ownerDeleteToken != null) {
            jdbcTemplate.update(
                    "UPDATE item_publication_version SET owner_delete_token = ? WHERE id = ?",
                    ownerDeleteToken,
                    newVersionId);
        }
        return true;
    }

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
        final StringBuilder sql = new StringBuilder("UPDATE item_publication_version"
                + " SET title = ?, description = ?, price_per_hour = ?, difficulty_level = ?,"
                + " location_option_id = ?,"
                + " location_name = (SELECT name FROM location_option WHERE id = ?),"
                + " cover_image_data = ?, created_at = CURRENT_TIMESTAMP"
                + " WHERE id = ?");
        args.add(title);
        args.add(description);
        args.add(pricePerHour);
        args.add(difficultyLevel);
        args.add(locationOptionId);
        args.add(locationOptionId);
        args.add(ItemPublicationCoverJdbcSupport.findCurrentCoverImageData(jdbcTemplate, itemId));
        args.add(currentVersionId);
        if (ownerId != null) {
            sql.append(" AND owner_id = ?");
            args.add(ownerId);
        }
        final String updateSql = Objects.requireNonNull(sql.toString());
        return jdbcTemplate.update(updateSql, args.toArray()) > 0;
    }

    private Optional<Map<String, Object>> findCurrentPublicationVersionData(final int itemId, final Integer ownerId) {
        final List<Object> args = new ArrayList<>();
        final StringBuilder sql =
                new StringBuilder("SELECT i.version_id AS current_version_id, v.owner_id, v.type_id, v.capacity_people,"
                        + " v.max_weight_kg, v.active, v.owner_delete_token, v.item_created_at"
                        + " FROM item i"
                        + " JOIN item_publication_version v ON v.id = i.version_id"
                        + " WHERE i.id = ?");
        args.add(itemId);
        if (ownerId != null) {
            sql.append(" AND v.owner_id = ?");
            args.add(ownerId);
        }
        final String selectSql = Objects.requireNonNull(sql.toString());
        return jdbcTemplate.queryForList(selectSql, args.toArray()).stream().findFirst();
    }

    private boolean currentVersionHasBookingReferences(final int currentVersionId) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_booking WHERE item_version_id = ?", Integer.class, currentVersionId);
        return count != null && count > 0;
    }

    private Integer findCurrentVersionIdByItemId(final int itemId) {
        return jdbcTemplate.queryForObject("SELECT version_id FROM item WHERE id = ?", Integer.class, itemId);
    }

    private int updateCurrentVersionActive(final int itemId, final Integer ownerId, final boolean active) {
        final List<Object> args = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("UPDATE item_publication_version"
                + " SET active = ?"
                + " WHERE id = (SELECT version_id FROM item WHERE id = ?)");
        args.add(active);
        args.add(itemId);
        if (ownerId != null) {
            sql.append(" AND owner_id = ?");
            args.add(ownerId);
        }
        final String updateActiveSql = Objects.requireNonNull(sql.toString());
        return jdbcTemplate.update(updateActiveSql, args.toArray());
    }

    private void clearCurrentVersionOwnerDeleteToken(final int itemId) {
        jdbcTemplate.update(
                "UPDATE item_publication_version SET owner_delete_token = NULL WHERE id = (SELECT version_id FROM item WHERE id = ?)",
                itemId);
    }

    private String findLocationNameById(final int locationOptionId) {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM location_option WHERE id = ?", String.class, locationOptionId);
    }

    /**
     * True when the item still has guest (non-owner) bookings that are not cancelled/rejected and have not ended yet,
     * so it cannot be removed from the database; an active listing is only deactivated instead.
     */
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
}
