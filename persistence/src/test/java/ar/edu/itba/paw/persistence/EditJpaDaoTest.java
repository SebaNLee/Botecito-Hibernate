package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static ar.edu.itba.paw.persistence.TestUtils.*;

import ar.edu.itba.paw.models.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class EditJpaDaoTest {

    @Autowired
    private EditDao editDao;

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
        host = insertUser(em, "Host", "User", "host@test.com");
        guest = insertUser(em, "Guest", "User", "guest@test.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        version = insertVersion(em, item, itemType, location,
                "Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        em.flush();
        em.clear();
    }

    @Test
    public void testItemHasBookingsNoVersion() {
        Item noVersionItem = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();
        em.clear();

        boolean result = editDao.itemHasBookings(noVersionItem.getId());

        assertFalse(result);
    }

    @Test
    public void testItemHasBookingsNoBookings() {
        boolean result = editDao.itemHasBookings(item.getId());

        assertFalse(result);
    }

    @Test
    public void testItemHasBookingsHasBookings() {
        insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        em.clear();

        boolean result = editDao.itemHasBookings(item.getId());

        assertTrue(result);
    }

    @Test
    public void testFindVersionByIdFound() {
        Optional<Version> result = editDao.findVersionById(version.getId());

        assertTrue(result.isPresent());
        assertEquals(version.getId(), result.get().getId());
        assertEquals("Boat", result.get().getTitle());
    }

    @Test
    public void testRemoveVersionChildrenAvailabilities() {
        Version managed = em.find(Version.class, version.getId());
        Availability availability = insertAvailability(em, managed, WeekdayEnum.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0));
        em.flush();
        em.clear();

        managed = em.find(Version.class, version.getId());
        editDao.removeVersionChildren(managed);
        em.flush();

        assertNull(em.find(Availability.class, availability.getId()));
    }

    @Test
    public void testRemoveVersionChildrenMedia() {
        Version managed = em.find(Version.class, version.getId());
        Availability availability = insertAvailability(em, managed, WeekdayEnum.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0));
        Image image = insertImage(em);
        Media media = insertMedia(em, managed, image, 0);
        em.flush();
        em.clear();

        managed = em.find(Version.class, version.getId());
        editDao.removeVersionChildren(managed);
        em.flush();

        assertNull(em.find(Availability.class, availability.getId()));
        assertNull(em.find(Media.class, media.getId()));
        assertNotNull(em.find(Image.class, image.getId()));
    }
}
