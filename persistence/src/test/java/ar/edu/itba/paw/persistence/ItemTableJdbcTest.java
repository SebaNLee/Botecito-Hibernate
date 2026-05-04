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
        final int versionId = insertPublicationVersion(ownerId, "item-a", 2000, 2, 1, null);

        jdbcTemplate.update("INSERT INTO item (version_id) VALUES (?)", versionId);

        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item i JOIN item_publication_version v ON v.id = i.version_id WHERE v.title = ?",
                Integer.class,
                "item-a");
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testCreateItemWhenOwnerDoesNotExist() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item_publication_version"
                                + " (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id, location_name, active, item_created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                        999999,
                        1,
                        "item-a",
                        2000,
                        2,
                        1,
                        locationName(1),
                        Boolean.TRUE));
    }

    @Test
    public void testCreateItemWhenPriceIsNegative() {
        final int ownerId = insertUser("a@a.com");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item_publication_version"
                                + " (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id, location_name, active, item_created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                        ownerId,
                        1,
                        "item-a",
                        -10,
                        2,
                        1,
                        locationName(1),
                        Boolean.TRUE));
    }

    @Test
    public void testCreateItemWhenDeleteTokenIsDuplicated() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int ownerId = insertUser("a@a.com");
        final String token = "t";
        insertPublicationVersion(ownerId, "item-a", 1500, 2, 1, token);

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO item_publication_version"
                                + " (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id, location_name, active, owner_delete_token, item_created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                        ownerId,
                        1,
                        "item-b",
                        1800,
                        2,
                        1,
                        locationName(1),
                        Boolean.TRUE,
                        token));
    }

    private int insertPublicationVersion(
            final int ownerId,
            final String title,
            final int pricePerHour,
            final int capacityPeople,
            final int locationOptionId,
            final String ownerDeleteToken) {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                "INSERT INTO item_publication_version"
                        + " (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id, location_name, active, owner_delete_token, item_created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                ownerId,
                1,
                title,
                pricePerHour,
                capacityPeople,
                locationOptionId,
                locationName(locationOptionId),
                Boolean.TRUE,
                ownerDeleteToken);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM item_publication_version WHERE owner_id = ? AND title = ? ORDER BY id DESC LIMIT 1",
                Integer.class,
                ownerId,
                title);
    }

    private String locationName(final int locationOptionId) {
        return jdbcTemplate()
                .queryForObject("SELECT name FROM location_option WHERE id = ?", String.class, locationOptionId);
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
