package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
@Transactional
public class BookingJpaDaoTest {

    @Autowired
    private BookingDao bookingDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Users guest;
    private Location location;
    private ItemType itemType;
    private Item item;
    private Version version;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        guest = insertUser(em, "Guest", "User", "botecito.user@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        version = insertVersion(em, item, itemType, location, "Test Boat");
        em.flush();
    }

    @Test
    public void testInsertBooking() {
        Booking booking = createBooking(
                version, guest, LocalDateTime.now(), LocalDateTime.now().plusDays(1), BookingStatusEnum.PENDING);
        bookingDao.insertBooking(booking);
        em.flush();

        assertNotNull(em.find(Booking.class, booking.getId()));
    }

    @Test
    public void testFindById() {
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();

        Optional<Booking> found = bookingDao.findById(booking.getId());

        assertTrue(found.isPresent());
        assertEquals(guest.getId(), found.get().getGuest().getId());
    }

    @Test
    public void testFindByIdNotFound() {
        assertFalse(bookingDao.findById(100000).isPresent());
    }

    @Test
    public void testDeleteBooking() {
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();
        int bookingId = booking.getId();

        assertTrue(bookingDao.deleteBooking(bookingId));
        em.flush();
        em.clear();

        assertNull(em.find(Booking.class, bookingId));
    }

    @Test
    public void testUploadPayment() {
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();

        PaymentProof proof = PaymentProof.builder()
                .booking(booking)
                .filename("proof.pdf")
                .contentType("application/pdf")
                .fileData(new byte[] {1, 2, 3})
                .createdAt(LocalDateTime.now())
                .build();
        bookingDao.uploadPayment(proof);
        em.flush();

        assertNotNull(em.find(PaymentProof.class, proof.getId()));
    }

    @Test
    public void testCanInsertBooking() {
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        Booking booking = createBooking(version, guest, start, start.plusDays(1), BookingStatusEnum.PENDING);

        assertTrue(bookingDao.canInsertBooking(
                booking, EnumSet.of(BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED)));
    }

    @Test
    public void testCannotInsertBooking() {
        insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking newBooking = createBooking(version, guest, start, start.plusDays(1), BookingStatusEnum.PENDING);

        assertFalse(bookingDao.canInsertBooking(
                newBooking, EnumSet.of(BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED)));
    }

    @Test
    public void testCanUpdate() {
        Booking existing = insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking update = createBooking(version, guest, start, start.plusDays(1), BookingStatusEnum.PENDING);

        assertTrue(bookingDao.canUpdate(
                update, existing.getId(), EnumSet.of(BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED)));
    }

    @Test
    public void testSearchBookingsAsGuest() {
        insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        em.flush();

        BookingQueryModel query = BookingQueryModel.builder()
                .callerId(guest.getId())
                .asHost(false)
                .sortBy("newest")
                .page(1)
                .pageSize(12)
                .build();

        SearchResult<Booking> result = bookingDao.searchBookings(query);

        assertEquals(guest.getId(), result.getPageElements().get(0).getGuest().getId());
    }

    @Test
    public void testSearchBookingsByStatus() {
        insertBooking(em, version, guest, BookingStatusEnum.PENDING);
        insertBooking(em, version, guest, BookingStatusEnum.ACCEPTED);
        em.flush();

        BookingQueryModel query = BookingQueryModel.builder()
                .callerId(guest.getId())
                .asHost(false)
                .status(BookingStatusEnum.ACCEPTED)
                .sortBy("newest")
                .page(1)
                .pageSize(12)
                .build();

        SearchResult<Booking> result = bookingDao.searchBookings(query);

        assertEquals(BookingStatusEnum.ACCEPTED, result.getPageElements().get(0).getStatus());
    }

    @Test
    public void testFinalizeBookingsBefore() {
        LocalDateTime pastEnd = LocalDateTime.now().minusDays(2);
        em.persist(createBooking(version, guest, pastEnd.minusDays(1), pastEnd, BookingStatusEnum.CONFIRMED));
        em.flush();

        bookingDao.finalizeBookingsBefore(LocalDateTime.now());
        em.flush();
        em.clear();

        long finalizedCount = (Long) em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
                .setParameter("status", BookingStatusEnum.FINISHED)
                .getSingleResult();

        assertEquals(1, finalizedCount);
    }

    @Test
    public void testExpireBookingsBefore() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(2);
        em.persist(createBooking(version, guest, pastStart, pastStart.plusDays(1), BookingStatusEnum.PENDING));
        em.flush();

        bookingDao.expireBookingsBefore(
                LocalDateTime.now(), EnumSet.of(BookingStatusEnum.PENDING, BookingStatusEnum.ACCEPTED));
        em.flush();
        em.clear();

        long cancelledCount = (Long) em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
                .setParameter("status", BookingStatusEnum.CANCELLED)
                .getSingleResult();

        assertEquals(1, cancelledCount);
    }

    @Test
    public void testDeleteSelfBlock() {
        Users sameUser = insertUser(em, "Same", "User", "botecito.admin@gmail.com");
        Item sameUserItem = insertItem(em, sameUser, ItemStatusEnum.ACTIVE);
        Version sameUserVersion = insertVersion(em, sameUserItem, itemType, location, "Self Boat");
        em.flush();

        LocalDateTime pastEnd = LocalDateTime.now().minusDays(2);
        em.persist(createBooking(sameUserVersion, sameUser, pastEnd.minusDays(1), pastEnd, BookingStatusEnum.ACCEPTED));
        em.flush();

        bookingDao.deleteSelfBlocksBefore(LocalDateTime.now());
        em.flush();
        em.clear();

        long remainingSelf = (Long) em.createQuery(
                        "SELECT COUNT(b) FROM Booking b WHERE b.guest.id = :userId AND b.version.item.host.id = :userId")
                .setParameter("userId", sameUser.getId())
                .getSingleResult();

        assertEquals(0, remainingSelf);
    }
}
