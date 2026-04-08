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

        jdbcTemplate.update(
                "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location) VALUES (?, ?, ?, ?, ?, ?)",
                ownerId,
                1,
                "item-a",
                2000,
                2,
                "Tigre");

        final Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item WHERE title = ?", Integer.class, "item-a");
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testCreateItemWhenOwnerDoesNotExist() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location) VALUES (?, ?, ?, ?, ?, ?)",
                        999999,
                        1,
                        "item-a",
                        2000,
                        2,
                        "Tigre"));
    }

    @Test
    public void testCreateItemWhenPriceIsNegative() {
        final int ownerId = insertUser("a@a.com");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location) VALUES (?, ?, ?, ?, ?, ?)",
                        ownerId,
                        1,
                        "item-a",
                        -10,
                        2,
                        "Tigre"));
    }

    @Test
    public void testCreateItemWhenDeleteTokenIsDuplicated() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int ownerId = insertUser("a@a.com");
        final String token = "t";
        jdbcTemplate.update(
                "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location, owner_delete_token) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ownerId,
                1,
                "item-a",
                1500,
                2,
                "Tigre",
                token);

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location, owner_delete_token) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        ownerId,
                        1,
                        "item-b",
                        1800,
                        2,
                        "Tigre",
                        token));
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
