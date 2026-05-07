package ar.edu.itba.paw.persistence;

import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ItemMediaTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateMediaWhenDataIsValid() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int itemId = insertItem("a@a.com", "item-a");
        final int imageId = insertItemImage(new byte[] {1, 2, 3});

        jdbcTemplate.update(
                "INSERT INTO media (version_id, image_id, index)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, 0)",
                itemId,
                imageId);

        Assertions.assertEquals(
                1,
                JdbcTestUtils.countRowsInTableWhere(
                        jdbcTemplate,
                        "media",
                        "version_id = (SELECT MAX(v.id) FROM version v WHERE v.item_id = " + itemId + ")"));
    }

    @Test
    public void testCreateMediaWhenImageIsMissing() {
        insertItem("a@a.com", "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("INSERT INTO image (data) VALUES (?)", (Object) null));
    }

    @Test
    public void testDeleteItemKeepsVersionReferencesAndImages() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int itemId = insertItem("a@a.com", "item-a");
        final int imageId = insertItemImage(new byte[] {4, 5, 6});
        jdbcTemplate.update(
                "INSERT INTO media (version_id, image_id, index)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, 0)",
                itemId,
                imageId);

        jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);

        Assertions.assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "media", "image_id = " + imageId));
        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + imageId));
    }

    @Test
    public void testDeleteItemAfterRemovingVersionReferencesKeepsImageBytes() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int itemId = insertItem("a@a.com", "item-b");
        final int imageId = insertItemImage(new byte[] {7, 8, 9});
        jdbcTemplate.update(
                "INSERT INTO media (version_id, image_id, index)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, 0)",
                itemId,
                imageId);

        jdbcTemplate.update(
                "DELETE FROM media WHERE version_id = (SELECT MAX(v.id) FROM version v WHERE v.item_id = ?)", itemId);
        jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);

        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + imageId));
    }

    private int insertItem(final String ownerEmail, final String itemTitle) {
        final int ownerId = insertUser(ownerEmail);
        jdbcTemplate()
                .update(
                        "INSERT INTO item (host_id, status, created_at) VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP)",
                        ownerId);
        final int itemId =
                Objects.requireNonNull(jdbcTemplate().queryForObject("SELECT MAX(id) FROM item", Integer.class));
        jdbcTemplate()
                .update(
                        "INSERT INTO version (item_id, type_id, title, description, price, capacity, weight, difficulty, location_id, timezone, created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UTC', CURRENT_TIMESTAMP)",
                        itemId,
                        1,
                        itemTitle,
                        "desc",
                        1200,
                        2,
                        2000,
                        1,
                        1);
        return itemId;
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private int insertItemImage(final byte[] data) {
        jdbcTemplate().update("INSERT INTO image (data) VALUES (?)", data);
        return Objects.requireNonNull(jdbcTemplate().queryForObject("SELECT MAX(id) FROM image", Integer.class));
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
