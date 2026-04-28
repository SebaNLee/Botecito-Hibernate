package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
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

    @Autowired
    private @NonNull ItemDao itemDao;

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

    @Test
    public void testPublicationEditSnapshotsActiveBookingsOnce() {
        final int ownerId = insertUser("owner@a.com");
        final int guestId = insertUser("guest@a.com");
        final int itemId = insertItem(ownerId, "snapshot-title");
        jdbcTemplate()
                .update(
                        "INSERT INTO item_media (item_id, image_data, display_order) VALUES (?, ?, ?)",
                        itemId,
                        new byte[] {1, 2, 3},
                        0);

        final OffsetDateTime start = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final ItemBooking booking =
                itemDao.createBookingRequest(itemId, guestId, start, start.plusHours(1), "message", "snapshot-token");
        Assertions.assertTrue(itemDao.findSnapshotByBookingIdForGuest(booking.getId(), guestId)
                .isEmpty());
        Assertions.assertTrue(itemDao.hasBlockingBookingsForEdition(itemId));

        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));
        Assertions.assertFalse(itemDao.hasBlockingBookingsForEdition(itemId));
        jdbcTemplate().update("UPDATE item SET title = ?, price_per_hour = ? WHERE id = ?", "changed", 9999, itemId);

        final ItemSnapshot snapshot = itemDao.findSnapshotByBookingIdForGuest(booking.getId(), guestId)
                .orElseThrow();
        Assertions.assertEquals("snapshot-title", snapshot.getTitle());
        Assertions.assertEquals(1500, snapshot.getPricePerHour());
        Assertions.assertArrayEquals(new byte[] {1, 2, 3}, snapshot.getCoverImageData());
        Assertions.assertTrue(itemDao.findSnapshotByBookingIdForGuest(booking.getId(), ownerId)
                .isEmpty());
        Assertions.assertTrue(itemDao.findSnapshotByBookingIdForOwner(booking.getId(), ownerId)
                .isPresent());
        Assertions.assertEquals(
                1, itemDao.listSnapshotsByItemIdForOwner(itemId, ownerId).size());
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));
        Assertions.assertEquals(
                1, itemDao.listSnapshotsByItemIdForOwner(itemId, ownerId).size());
    }

    @Test
    public void testPublicationEditSnapshotsCompletedBooking() {
        final int ownerId = insertUser("owner-completed@a.com");
        final int guestId = insertUser("guest-completed@a.com");
        final int itemId = insertItem(ownerId, "completed-snapshot-title");
        final OffsetDateTime start = OffsetDateTime.parse("2026-01-03T10:00:00Z");
        final ItemBooking booking = itemDao.createBookingRequest(
                itemId, guestId, start, start.plusHours(1), "message", "completed-snapshot-token");
        jdbcTemplate().update("UPDATE item_booking SET state = ? WHERE id = ?", "BOOKING_COMPLETED", booking.getId());

        Assertions.assertFalse(itemDao.hasBlockingBookingsForEdition(itemId));
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));

        final ItemSnapshot snapshot = itemDao.findSnapshotByBookingIdForGuest(booking.getId(), guestId)
                .orElseThrow();
        Assertions.assertEquals("completed-snapshot-title", snapshot.getTitle());
    }

    @Test
    public void testBookingVersionMustBelongToSameItem() {
        final int ownerId = insertUser("owner-version-fk@a.com");
        final int guestId = insertUser("guest-version-fk@a.com");
        final int snapshotItemId = insertItem(ownerId, "snapshot-source-item");
        final int otherItemId = insertItem(ownerId, "snapshot-target-item");
        final OffsetDateTime start = OffsetDateTime.parse("2026-01-04T10:00:00Z");
        itemDao.createBookingRequest(snapshotItemId, guestId, start, start.plusHours(1), "message", "source-token");
        final ItemBooking otherBooking = itemDao.createBookingRequest(
                otherItemId, guestId, start.plusHours(2), start.plusHours(3), "message", "target-token");
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(snapshotItemId));
        final Integer versionId = jdbcTemplate()
                .queryForObject(
                        "SELECT item_version_id FROM item_booking WHERE item_id = ?", Integer.class, snapshotItemId);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("UPDATE item_booking SET item_version_id = ? WHERE id = ?", versionId, otherBooking.getId()));
    }

    @Test
    public void testDeletePublicationWithoutBookingsRemovesItem() {
        final int ownerId = insertUser("owner-delete@a.com");
        final int itemId = insertItem(ownerId, "delete-without-bookings");

        Assertions.assertTrue(itemDao.deleteItemById(itemId));

        Assertions.assertTrue(itemDao.findAnyItemById(itemId).isEmpty());
    }

    @Test
    public void testDeletePublicationWithVersionedBookingHidesItemAndPreservesSnapshot() {
        final int ownerId = insertUser("owner-delete-versioned@a.com");
        final int guestId = insertUser("guest-delete-versioned@a.com");
        final int itemId = insertItem(ownerId, "delete-versioned-booking");
        final OffsetDateTime start = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ItemBooking booking = itemDao.createBookingRequest(
                itemId, guestId, start, start.plusHours(1), "message", "delete-versioned-token");
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));

        Assertions.assertTrue(itemDao.deleteItemById(itemId));

        Assertions.assertTrue(itemDao.listItemsByOwnerId(ownerId).isEmpty());
        Assertions.assertTrue(itemDao.findAnyItemById(itemId).isPresent());
        Assertions.assertTrue(itemDao.findSnapshotByBookingIdForOwner(booking.getId(), ownerId)
                .isPresent());
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private int insertItem(final int ownerId, final String title) {
        jdbcTemplate()
                .update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id) VALUES (?, ?, ?, ?, ?, ?)",
                        ownerId,
                        1,
                        title,
                        1500,
                        2,
                        1);
        return jdbcTemplate().queryForObject("SELECT id FROM item WHERE title = ?", Integer.class, title);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
