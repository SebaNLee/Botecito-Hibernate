package ar.edu.itba.paw.persistence;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemMediaJdbcDao implements ItemMediaDao {

    private final @NonNull JdbcTemplate jdbcTemplate;

    public ItemMediaJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
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

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData, final int displayOrder) {
        final SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("item_media").usingGeneratedKeyColumns("id");
        final Map<String, Object> args = new HashMap<>();
        args.put("item_id", itemId);
        args.put("image_data", imageData);
        args.put("display_order", displayOrder);
        final Integer imageId = insert.executeAndReturnKey(args).intValue();
        ItemPublicationCoverJdbcSupport.syncCurrentPublicationVersionCoverImage(jdbcTemplate, itemId);
        return imageId;
    }

    @Override
    public boolean deleteImage(final int itemId, final int imageId) {
        final List<Integer> currentOrder = jdbcTemplate.queryForList(
                "SELECT id FROM item_media WHERE item_id = ? ORDER BY display_order ASC", Integer.class, itemId);
        if (!currentOrder.contains(imageId)) {
            return false;
        }
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
        ItemPublicationCoverJdbcSupport.syncCurrentPublicationVersionCoverImage(jdbcTemplate, itemId);
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
        ItemPublicationCoverJdbcSupport.syncCurrentPublicationVersionCoverImage(jdbcTemplate, itemId);
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
        if (updatedRows > 0) {
            ItemPublicationCoverJdbcSupport.syncCurrentPublicationVersionCoverImage(jdbcTemplate, itemId);
        }
        return updatedRows > 0 ? existingImageId : null;
    }

    private boolean hasTable(final String tableName) {
        final Boolean hasTable = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
                return tables.next();
            }
        });
        return Boolean.TRUE.equals(hasTable);
    }
}
