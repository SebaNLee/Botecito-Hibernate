package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.time.OffsetDateTime;
import java.util.Objects;
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
    private @NonNull ItemBookingDao itemBookingDao;

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testEditCreatesSnapshot() {
        final int ownerId = insertUser("owner@a.com");
        final int guestId = insertUser("guest@a.com");
        final int itemId = insertItem(ownerId, "snapshot-title");
        jdbcTemplate().update("INSERT INTO image (data) VALUES (?)", new byte[] {1, 2, 3});
        final int imageId =
                Objects.requireNonNull(jdbcTemplate().queryForObject("SELECT MAX(id) FROM image", Integer.class));
        jdbcTemplate()
                .update(
                        "INSERT INTO media (version_id, image_id, index)"
                                + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, 0)",
                        itemId,
                        imageId);

        final OffsetDateTime start = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final ItemBooking booking = itemBookingDao.createBookingRequest(
                itemId, guestId, start, start.plusHours(1), "message", "snapshot-token");

        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));
        Assertions.assertTrue(itemDao.snapshotBookingsForPublicationEdit(itemId));

        final ItemSnapshot snapshot = itemBookingDao
                .findSnapshotByBookingIdForGuest(booking.getId(), guestId)
                .orElseThrow();
        Assertions.assertEquals("snapshot-title", snapshot.getTitle());
        Assertions.assertEquals(
                1, itemBookingDao.listSnapshotsByItemIdForOwner(itemId, ownerId).size());
    }

    @Test
    public void testDeleteWithoutBookings() {
        final int ownerId = insertUser("owner-delete@a.com");
        final int itemId = insertItem(ownerId, "delete-without-bookings");

        Assertions.assertTrue(itemDao.deleteItemById(itemId));
        Assertions.assertTrue(itemDao.findAnyItemById(itemId).isEmpty());
    }

    @Test
    public void testDeleteInactiveWithBookings() {
        final int ownerId = insertUser("owner-delete-future@a.com");
        final int guestId = insertUser("guest-delete-future@a.com");
        final int itemId = insertItem(ownerId, "delete-future-booking");
        final OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        itemBookingDao.createBookingRequest(
                itemId, guestId, start, start.plusHours(1), "message", "delete-future-token");

        Assertions.assertTrue(itemDao.deleteItemById(itemId));
        Assertions.assertFalse(itemDao.deleteItemById(itemId));
    }

    @Test
    public void testExpireDueBookings() {
        final int ownerId = insertUser("owner-expire@a.com");
        final int guestId = insertUser("guest-expire@a.com");
        final int itemId = insertItem(ownerId, "expire-bookings");
        final OffsetDateTime threshold = OffsetDateTime.parse("2026-01-10T10:00:00Z");

        final ItemBooking pastBooking = itemBookingDao.createBookingRequest(
                itemId, guestId, threshold.minusHours(2), threshold.minusHours(1), "message", "expire-past-token");
        final ItemBooking futureBooking = itemBookingDao.createBookingRequest(
                itemId, guestId, threshold.plusHours(1), threshold.plusHours(2), "message", "expire-future-token");

        itemBookingDao.expireAllDueBookings(threshold);

        Assertions.assertEquals(
                BookingState.BOOKING_CANCELLED,
                itemBookingDao
                        .findBookingById(pastBooking.getId())
                        .orElseThrow()
                        .getState());
        Assertions.assertEquals(
                BookingState.BOOKING_PENDING,
                itemBookingDao
                        .findBookingById(futureBooking.getId())
                        .orElseThrow()
                        .getState());
    }

    @Test
    public void testListBookingsByGuestIdExcludesOwnerSelfBlocks() {
        final int ownerId = insertUser("owner-self-guest@a.com");
        final int externalGuestId = insertUser("external-self-guest@a.com");
        final int itemId = insertItem(ownerId, "self-guest-list");
        final OffsetDateTime start = OffsetDateTime.now().plusDays(2);

        final ItemBooking externalBooking = itemBookingDao.createBookingRequest(
                itemId, externalGuestId, start, start.plusHours(1), "external booking", "external-self-guest-token");
        itemBookingDao.insertOwnerPersonalBlock(
                itemId,
                ownerId,
                start.plusHours(2),
                start.plusHours(3),
                "owner-self-guest-token",
                OffsetDateTime.now());

        final var ownerGuestBookings = itemBookingDao.listBookingsByGuestId(ownerId);
        final var externalGuestBookings = itemBookingDao.listBookingsByGuestId(externalGuestId);

        Assertions.assertTrue(ownerGuestBookings.isEmpty());
        Assertions.assertEquals(1, externalGuestBookings.size());
        Assertions.assertEquals(
                externalBooking.getId(), externalGuestBookings.getFirst().getId());
    }

    @Test
    public void testListBookingsByOwnerIdExcludesOwnerSelfBlocks() {
        final int ownerId = insertUser("owner-self-owner@a.com");
        final int externalGuestId = insertUser("external-self-owner@a.com");
        final int itemId = insertItem(ownerId, "self-owner-list");
        final OffsetDateTime start = OffsetDateTime.now().plusDays(3);

        final ItemBooking externalBooking = itemBookingDao.createBookingRequest(
                itemId, externalGuestId, start, start.plusHours(1), "external booking", "external-self-owner-token");
        itemBookingDao.insertOwnerPersonalBlock(
                itemId,
                ownerId,
                start.plusHours(2),
                start.plusHours(3),
                "owner-self-owner-token",
                OffsetDateTime.now());

        final var ownerBookings = itemBookingDao.listBookingsByOwnerId(ownerId);

        Assertions.assertEquals(1, ownerBookings.size());
        Assertions.assertEquals(
                externalBooking.getId(), ownerBookings.getFirst().getId());
    }

    @Test
    public void testDeleteOwnerSelfBlockRemovesRow() {
        final int ownerId = insertUser("owner-delete-self@a.com");
        final int itemId = insertItem(ownerId, "delete-self-block");
        final OffsetDateTime start = OffsetDateTime.now().plusDays(4);

        final ItemBooking selfBlock = itemBookingDao.insertOwnerPersonalBlock(
                itemId, ownerId, start, start.plusHours(1), "delete-self-block-token", OffsetDateTime.now());

        Assertions.assertTrue(itemBookingDao.findBookingById(selfBlock.getId()).isPresent());
        Assertions.assertTrue(itemBookingDao.deleteOwnerSelfBlock(selfBlock.getId(), ownerId));
        Assertions.assertTrue(itemBookingDao.findBookingById(selfBlock.getId()).isEmpty());
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private int insertItem(final int ownerId, final String title) {
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
