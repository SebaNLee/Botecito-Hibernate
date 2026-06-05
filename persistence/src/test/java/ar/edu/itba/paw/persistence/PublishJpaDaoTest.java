package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static ar.edu.itba.paw.persistence.TestUtils.*;

import ar.edu.itba.paw.models.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
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

        Version version = new Version();
        version.setItem(item);
        version.setType(itemType);
        version.setLocation(location);
        version.setTitle("Boat");
        version.setPrice(BigDecimal.valueOf(100));
        version.setCapacity(4);
        version.setWeight(200);
        version.setDifficulty(2);
        version.setTimezone("America/Argentina/Buenos_Aires");
        version.setCreatedAt(LocalDateTime.now());

        Version persisted = publishDao.persistVersion(version);

        assertNotNull(persisted.getId());
        assertNotNull(em.find(Version.class, persisted.getId()));
    }

    @Test
    public void testPersistAvailability() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Availability availability = new Availability();
        availability.setVersion(version);
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
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Image image = insertImage(em);
        em.flush();

        Media media = new Media();
        media.setId(new MediaId(version.getId(), 0));
        media.setVersion(version);
        media.setImage(image);

        Media persisted = publishDao.persistMedia(media);

        assertNotNull(em.find(Media.class, persisted.getId()));
    }
}
