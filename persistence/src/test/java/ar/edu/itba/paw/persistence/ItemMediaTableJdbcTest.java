package ar.edu.itba.paw.persistence;

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

        jdbcTemplate.update(
                "INSERT INTO item_media (item_id, image_data, display_order) VALUES (?, ?, 0)",
                itemId,
                new byte[] {1, 2, 3});

        Assertions.assertEquals(
                1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "item_media", "item_id = " + itemId));
    }

    @Test
    public void testCreateMediaWhenImageIsMissing() {
        final int itemId = insertItem("a@a.com", "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("INSERT INTO item_media (item_id, image_data, display_order) VALUES (?, ?, 0)", itemId, null));
    }

    @Test
    public void testDeleteItemAlsoDeletesMedia() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int itemId = insertItem("a@a.com", "item-a");
        jdbcTemplate.update(
                "INSERT INTO item_media (item_id, image_data, display_order) VALUES (?, ?, 0)",
                itemId,
                new byte[] {4, 5, 6});

        jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);

        Assertions.assertEquals(
                0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "item_media", "item_id = " + itemId));
    }

    private int insertItem(final String ownerEmail, final String itemTitle) {
        final int ownerId = insertUser(ownerEmail);
        final String locationName =
                jdbcTemplate().queryForObject("SELECT name FROM location_option WHERE id = ?", String.class, 1);
        jdbcTemplate()
                .update(
                        "INSERT INTO item_publication_version"
                                + " (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id, location_name, active, item_created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                        ownerId,
                        1,
                        itemTitle,
                        1200,
                        2,
                        1,
                        locationName,
                        Boolean.TRUE);
        final int versionId = jdbcTemplate()
                .queryForObject(
                        "SELECT id FROM item_publication_version WHERE owner_id = ? AND title = ? ORDER BY id DESC LIMIT 1",
                        Integer.class,
                        ownerId,
                        itemTitle);
        jdbcTemplate().update("INSERT INTO item (version_id) VALUES (?)", versionId);
        return jdbcTemplate()
                .queryForObject(
                        "SELECT i.id FROM item i JOIN item_publication_version v ON v.id = i.version_id WHERE v.id = ?",
                        Integer.class,
                        versionId);
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
