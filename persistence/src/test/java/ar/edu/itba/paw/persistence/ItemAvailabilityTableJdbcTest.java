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
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ItemAvailabilityTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateAvailability() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int itemId = insertItem("a@a.com", "item-a");

        jdbcTemplate.update(
                "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, ?, ?)",
                itemId,
                "MONDAY",
                "09:00:00",
                "11:00:00");

        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM availability WHERE version_id = (SELECT MAX(v.id) FROM version v WHERE v.item_id = ?)",
                Integer.class,
                itemId);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testAvailabilityWeekdayMissing() {
        final int itemId = insertItem("a@a.com", "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO availability (version_id, start_time, end_time)"
                                + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, ?)",
                        itemId,
                        "09:00:00",
                        "11:00:00"));
    }

    @Test
    public void testAvailabilityInvalidTime() {
        final int itemId = insertItem("a@a.com", "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                                + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, ?, ?)",
                        itemId,
                        "MONDAY",
                        "12:00:00",
                        "11:00:00"));
    }

    private int insertItem(final String ownerEmail, final String itemTitle) {
        final int ownerId = insertUser(ownerEmail);
        jdbcTemplate()
                .update(
                        "INSERT INTO item (host_id, status, created_at) VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP)",
                        ownerId);
        final int itemId = jdbcTemplate().queryForObject("SELECT MAX(id) FROM item", Integer.class);
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

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
