package ar.edu.itba.paw.persistence;

import java.sql.Timestamp;
import java.time.Instant;
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
public class ItemBookingTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateBookingWhenDataIsValid() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        jdbcTemplate.update(
                "INSERT INTO item_booking (item_id, guest_id, start_time, end_time, host_decision_token) VALUES (?, ?, ?, ?, ?)",
                itemId,
                guestId,
                Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                Timestamp.from(Instant.parse("2026-01-01T11:00:00Z")),
                "t-a");

        final String state = jdbcTemplate.queryForObject(
                "SELECT state FROM item_booking WHERE host_decision_token = ?", String.class, "t-a");
        Assertions.assertEquals("BOOKING_PENDING", state);
    }

    @Test
    public void testCreateBookingWhenTokenIsMissing() {
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item_booking (item_id, guest_id, start_time, end_time) VALUES (?, ?, ?, ?)",
                        itemId,
                        guestId,
                        Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                        Timestamp.from(Instant.parse("2026-01-01T11:00:00Z"))));
    }

    @Test
    public void testCreateBookingWhenTimeRangeIsInvalid() {
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO item_booking (item_id, guest_id, start_time, end_time, host_decision_token) VALUES (?, ?, ?, ?, ?)",
                        itemId,
                        guestId,
                        Timestamp.from(Instant.parse("2026-01-01T12:00:00Z")),
                        Timestamp.from(Instant.parse("2026-01-01T11:00:00Z")),
                        "t-a"));
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private int insertItem(final int ownerId, final String title) {
        jdbcTemplate()
                .update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location) VALUES (?, ?, ?, ?, ?, ?)",
                        ownerId,
                        1,
                        title,
                        1500,
                        2,
                        "Tigre");
        return jdbcTemplate().queryForObject("SELECT id FROM item WHERE title = ?", Integer.class, title);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
