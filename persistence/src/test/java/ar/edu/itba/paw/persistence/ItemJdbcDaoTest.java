package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ItemJdbcDaoTest {

    @Autowired
    private @NonNull ItemDao itemDao;

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testPublicationEditCreatesSingleSnapshotForActiveBooking() {
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

        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));

        final ItemSnapshot snapshot = itemDao.findSnapshotByBookingIdForGuest(booking.getId(), guestId)
                .orElseThrow();
        Assertions.assertEquals("snapshot-title", snapshot.getTitle());
        Assertions.assertEquals(
                1, itemDao.listSnapshotsByItemIdForOwner(itemId, ownerId).size());
    }

    @Test
    public void testDeletePublicationWithoutBookingsRemovesItem() {
        final int ownerId = insertUser("owner-delete@a.com");
        final int itemId = insertItem(ownerId, "delete-without-bookings");

        Assertions.assertTrue(itemDao.deleteItemById(itemId));
        Assertions.assertTrue(itemDao.findAnyItemById(itemId).isEmpty());
    }

    @Test
    public void testDeleteInactivePublicationWithFutureBookingIsBlocked() {
        final int ownerId = insertUser("owner-delete-future@a.com");
        final int guestId = insertUser("guest-delete-future@a.com");
        final int itemId = insertItem(ownerId, "delete-future-booking");
        final OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        itemDao.createBookingRequest(itemId, guestId, start, start.plusHours(1), "message", "delete-future-token");

        Assertions.assertTrue(itemDao.deleteItemById(itemId));
        Assertions.assertFalse(itemDao.deleteItemById(itemId));
    }

    @Test
    public void testExpireAllDueBookingsCancelsOnlyPastBookings() {
        final int ownerId = insertUser("owner-expire@a.com");
        final int guestId = insertUser("guest-expire@a.com");
        final int itemId = insertItem(ownerId, "expire-bookings");
        final OffsetDateTime threshold = OffsetDateTime.parse("2026-01-10T10:00:00Z");

        final ItemBooking pastBooking = itemDao.createBookingRequest(
                itemId, guestId, threshold.minusHours(2), threshold.minusHours(1), "message", "expire-past-token");
        final ItemBooking futureBooking = itemDao.createBookingRequest(
                itemId, guestId, threshold.plusHours(1), threshold.plusHours(2), "message", "expire-future-token");

        itemDao.expireAllDueBookings(threshold);

        Assertions.assertEquals(
                BookingState.BOOKING_CANCELLED,
                itemDao.findBookingById(pastBooking.getId()).orElseThrow().getState());
        Assertions.assertEquals(
                BookingState.BOOKING_PENDING,
                itemDao.findBookingById(futureBooking.getId()).orElseThrow().getState());
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
