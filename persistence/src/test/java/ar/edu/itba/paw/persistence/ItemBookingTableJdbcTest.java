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
    public void testCreateBooking() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        jdbcTemplate.update(
                "INSERT INTO booking"
                        + " (version_id, guest_id, start, \"end\", status, msg, created_at, updated_at)"
                        + " VALUES ((SELECT MAX(v.id) FROM \"version\" v WHERE v.item_id = ?), ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                itemId,
                guestId,
                Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                Timestamp.from(Instant.parse("2026-01-01T11:00:00Z")),
                "t-a");

        final String state =
                jdbcTemplate.queryForObject("SELECT status FROM booking WHERE msg = ?", String.class, "t-a");
        Assertions.assertEquals("PENDING", state);
    }

    @Test
    public void testBookingTokenMissing() {
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO booking (version_id, guest_id, start, \"end\")"
                                + " VALUES ((SELECT MAX(v.id) FROM \"version\" v WHERE v.item_id = ?), ?, ?, ?)",
                        itemId,
                        guestId,
                        Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                        Timestamp.from(Instant.parse("2026-01-01T11:00:00Z"))));
    }

    @Test
    public void testBookingInvalidTime() {
        final int ownerId = insertUser("a@a.com");
        final int guestId = insertUser("b@b.com");
        final int itemId = insertItem(ownerId, "item-a");

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO booking (version_id, guest_id, start, \"end\", status, msg, created_at, updated_at)"
                                + " VALUES ((SELECT MAX(v.id) FROM \"version\" v WHERE v.item_id = ?), ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        itemId,
                        guestId,
                        Timestamp.from(Instant.parse("2026-01-01T12:00:00Z")),
                        Timestamp.from(Instant.parse("2026-01-01T11:00:00Z")),
                        "t-a"));
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO \"user\" (first_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM \"user\" WHERE email = ?", Integer.class, email);
    }

    private int insertItem(final int ownerId, final String title) {
        jdbcTemplate()
                .update(
                        "INSERT INTO item (host_id, status, created_at) VALUES (?, 'ACTIVE', CURRENT_TIMESTAMP)",
                        ownerId);
        final int itemId = jdbcTemplate().queryForObject("SELECT MAX(id) FROM item", Integer.class);
        jdbcTemplate()
                .update(
                        "INSERT INTO \"version\" (item_id, type_id, title, description, price, capacity, weight, difficulty, location_id, timezone, created_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UTC', CURRENT_TIMESTAMP)",
                        itemId,
                        1,
                        title,
                        "desc",
                        1500,
                        2,
                        2000,
                        1,
                        1);
        return itemId;
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
