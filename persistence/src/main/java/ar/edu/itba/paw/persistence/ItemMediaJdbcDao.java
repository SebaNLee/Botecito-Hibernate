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
        if (!hasTable("image")) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                "SELECT data FROM image WHERE id = ?",
                rs -> {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getBytes("data"));
                    }
                    return Optional.<byte[]>empty();
                },
                id);
    }

    @Override
    public List<Integer> listImageIdsByItemIdOrdered(final int itemId) {
        if (!hasTable("media")) {
            return Collections.emptyList();
        }
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT image_id FROM media WHERE version_id = ? ORDER BY index ASC, image_id ASC",
                Integer.class,
                currentVersionId);
    }

    @Override
    public Optional<Integer> findCoverImageIdByItemId(final int itemId) {
        if (!hasTable("media")) {
            return Optional.empty();
        }
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return Optional.empty();
        }
        return jdbcTemplate
                .queryForList(
                        "SELECT image_id FROM media WHERE version_id = ? ORDER BY index ASC, image_id ASC LIMIT 1",
                        Integer.class,
                        currentVersionId)
                .stream()
                .findFirst();
    }

    @Override
    public int countImagesByItemId(final int itemId) {
        if (!hasTable("media")) {
            return 0;
        }
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return 0;
        }
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media WHERE version_id = ?", Integer.class, currentVersionId);
        return count == null ? 0 : count;
    }

    @Override
    public Integer insertImage(final int itemId, final byte[] imageData, final int displayOrder) {
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return null;
        }
        final SimpleJdbcInsert imageInsert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("image").usingGeneratedKeyColumns("id");
        final Map<String, Object> imageArgs = new HashMap<>();
        imageArgs.put("data", imageData);
        final Integer imageId = imageInsert.executeAndReturnKey(imageArgs).intValue();

        final SimpleJdbcInsert mediaInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("media");
        final Map<String, Object> mediaArgs = new HashMap<>();
        mediaArgs.put("version_id", currentVersionId);
        mediaArgs.put("image_id", imageId);
        mediaArgs.put("index", displayOrder);
        mediaInsert.execute(mediaArgs);
        return imageId;
    }

    @Override
    public boolean deleteImage(final int itemId, final int imageId) {
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return false;
        }
        final List<Integer> currentOrder = jdbcTemplate.queryForList(
                "SELECT image_id FROM media WHERE version_id = ? ORDER BY index ASC, image_id ASC",
                Integer.class,
                currentVersionId);
        if (!currentOrder.contains(imageId)) {
            return false;
        }
        final int deleted = jdbcTemplate.update(
                "DELETE FROM media WHERE version_id = ? AND image_id = ?", currentVersionId, imageId);
        if (deleted == 0) {
            return false;
        }
        int newPosition = 0;
        for (final Integer survivingId : currentOrder) {
            if (survivingId.intValue() == imageId) {
                continue;
            }
            jdbcTemplate.update(
                    "UPDATE media SET index = ? WHERE version_id = ? AND image_id = ?",
                    newPosition,
                    currentVersionId,
                    survivingId);
            newPosition++;
        }
        jdbcTemplate.update(
                "DELETE FROM image ii"
                        + " WHERE ii.id = ?"
                        + " AND NOT EXISTS (SELECT 1 FROM media im WHERE im.image_id = ii.id)",
                imageId);
        return true;
    }

    @Override
    public void reorderImages(final int itemId, final List<Integer> imageIdsInOrder) {
        if (imageIdsInOrder == null || imageIdsInOrder.isEmpty()) {
            return;
        }
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return;
        }
        for (int position = 0; position < imageIdsInOrder.size(); position++) {
            jdbcTemplate.update(
                    "UPDATE media SET index = ? WHERE version_id = ? AND image_id = ?",
                    position,
                    currentVersionId,
                    imageIdsInOrder.get(position));
        }
    }

    @Override
    public Integer replacePrimaryImage(final int itemId, final byte[] imageData) {
        if (!hasTable("media")) {
            return null;
        }
        final Integer currentVersionId = findCurrentVersionId(itemId);
        if (currentVersionId == null) {
            return null;
        }
        final Integer existingImageId = jdbcTemplate
                .queryForList(
                        "SELECT image_id FROM media WHERE version_id = ? ORDER BY index ASC, image_id ASC",
                        Integer.class,
                        currentVersionId)
                .stream()
                .findFirst()
                .orElse(null);
        if (existingImageId == null) {
            return insertImage(itemId, imageData, 0);
        }
        final int updatedRows =
                jdbcTemplate.update("UPDATE image SET data = ? WHERE id = ?", imageData, existingImageId);
        return updatedRows > 0 ? existingImageId : null;
    }

    private Integer findCurrentVersionId(final int itemId) {
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM \"version\" WHERE item_id = ?", Integer.class, itemId);
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
