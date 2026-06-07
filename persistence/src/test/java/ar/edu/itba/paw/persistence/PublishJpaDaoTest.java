package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

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
public class PublishJpaDaoTest {

    @Autowired
    private PublishDao publishDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Location location;
    private ItemType itemType;
    private Version version;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        version = insertVersion(
                em, item, itemType, location, "Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        em.flush();
    }

    @Test
    public void testPersistItem() {
        Item item = new Item();
        item.setHost(host);
        item.setStatus(ItemStatusEnum.ACTIVE);
        item.setCreatedAt(LocalDateTime.now());

        Item persisted = publishDao.persistItem(item);

        assertNotNull(persisted.getId());
        assertNotNull(em.find(Item.class, persisted.getId()));
    }

    @Test
    public void testPersistVersion() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();

        Version v = new Version();
        v.setItem(item);
        v.setType(itemType);
        v.setLocation(location);
        v.setTitle("Boat");
        v.setPrice(BigDecimal.valueOf(100));
        v.setCapacity(4);
        v.setWeight(200);
        v.setDifficulty(2);
        v.setTimezone("America/Argentina/Buenos_Aires");
        v.setCreatedAt(LocalDateTime.now());

        Version persisted = publishDao.persistVersion(v);

        assertNotNull(persisted.getId());
        assertNotNull(em.find(Version.class, persisted.getId()));
    }

    @Test
    public void testPersistAvailability() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Availability availability = new Availability();
        availability.setVersion(v);
        availability.setWeekday(WeekdayEnum.MONDAY);
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(17, 0));

        Availability persisted = publishDao.persistAvailability(availability);

        assertNotNull(persisted.getId());
        assertNotNull(em.find(Availability.class, persisted.getId()));
    }

    @Test
    public void testPersistMedia() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(em, item, itemType, location, "Boat");
        Image image = insertImage(em);
        em.flush();

        Media media = new Media();
        media.setId(new MediaId(v.getId(), 0));
        media.setVersion(v);
        media.setImage(image);

        Media persisted = publishDao.persistMedia(media);

        assertNotNull(em.find(Media.class, persisted.getId()));
    }

    @Test
    public void testFindVersionByIdFound() {
        Optional<Version> result = publishDao.findVersionById(version.getId());

        assertTrue(result.isPresent());
        assertEquals(version.getId(), result.get().getId());
        assertEquals("Boat", result.get().getTitle());
    }

    @Test
    public void testRemoveVersionChildrenAvailabilities() {
        Version managed = em.find(Version.class, version.getId());
        Availability availability =
                insertAvailability(em, managed, WeekdayEnum.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        em.flush();
        em.clear();

        managed = em.find(Version.class, version.getId());
        publishDao.removeVersionChildren(managed);
        em.flush();

        assertNull(em.find(Availability.class, availability.getId()));
    }

    @Test
    public void testRemoveVersionChildrenMedia() {
        Version managed = em.find(Version.class, version.getId());
        Availability availability =
                insertAvailability(em, managed, WeekdayEnum.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Image image = insertImage(em);
        Media media = insertMedia(em, managed, image, 0);
        em.flush();
        em.clear();

        managed = em.find(Version.class, version.getId());
        publishDao.removeVersionChildren(managed);
        em.flush();

        assertNull(em.find(Availability.class, availability.getId()));
        assertNull(em.find(Media.class, media.getId()));
        assertNotNull(em.find(Image.class, image.getId()));
    }
}
