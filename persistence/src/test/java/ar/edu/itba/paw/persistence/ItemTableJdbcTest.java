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
public class ItemTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateItemWhenDataIsValid() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int ownerId = insertUser("a@a.com");
        final int itemId = insertItem(ownerId, "item-a", 2000, 2, 1);

        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item i JOIN version v ON v.item_id = i.id WHERE v.title = ? AND i.id = ?",
                Integer.class,
                "item-a",
                itemId);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testCreateItemWhenOwnerDoesNotExist() {
        // Owner FK is now on item table.
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> insertItem(999999, "item-a", 2000, 2, 1));
    }

    @Test
    public void testCreateItemWhenPriceIsNegative() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO version"
                                + " (item_id, type_id, title, description, price, capacity, weight, difficulty, location_id, timezone, created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UTC', CURRENT_TIMESTAMP)",
                        insertItem(insertUser("owner-neg@a.com"), "owner-item", 1000, 2, 1),
                        1,
                        "item-a",
                        "desc",
                        -10.0,
                        2,
                        2000,
                        1,
                        1));
    }

    @Test
    public void testCreateItemWhenDeleteTokenIsDuplicated() {
        final int ownerId = insertUser("a@a.com");
        final int firstItemId = insertItem(ownerId, "item-a", 1500, 2, 1);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item (id, host_id, status, created_at) VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP)",
                        firstItemId,
                        ownerId));
    }

    private int insertItem(
            final int ownerId,
            final String title,
            final int pricePerHour,
            final int capacityPeople,
            final int locationOptionId) {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                "INSERT INTO item (host_id, status, created_at) VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP)", ownerId);
        final int itemId = java.util.Objects.requireNonNull(
                jdbcTemplate.queryForObject("SELECT MAX(id) FROM item", Integer.class));
        jdbcTemplate.update(
                "INSERT INTO version"
                        + " (item_id, type_id, title, description, price, capacity, weight, difficulty, location_id, timezone, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UTC', CURRENT_TIMESTAMP)",
                itemId,
                1,
                title,
                "desc",
                (double) pricePerHour,
                capacityPeople,
                2000,
                1,
                locationOptionId);
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
