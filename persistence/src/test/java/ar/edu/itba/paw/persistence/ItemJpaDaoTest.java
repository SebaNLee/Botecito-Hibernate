package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
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
public class ItemJpaDaoTest {

    @Autowired
    private ItemDao itemDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Users otherHost;
    private Location location;
    private ItemType itemType;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        otherHost = insertUser(em, "Other", "Host", "botecito.user@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        em.flush();
    }

    @Test
    public void testListOwnerItems() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        MyBoatsQueryModel query = MyBoatsQueryModel.builder()
                .ownerId(host.getId())
                .page(1)
                .pageSize(12)
                .build();

        PageModel<Item> result = itemDao.listOwnerItems(query);

        assertEquals(1, result.getTotalItems());
        assertEquals(1, result.getContent().size());
        assertEquals(item.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testListOwnerItemsDeleted() {
        Item active = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item deleted = insertItem(em, host, ItemStatusEnum.DELETED);
        insertVersion(em, active, itemType, location, "Active Boat");
        insertVersion(em, deleted, itemType, location, "Deleted Boat");
        em.flush();

        MyBoatsQueryModel query = MyBoatsQueryModel.builder()
                .ownerId(host.getId())
                .page(1)
                .pageSize(12)
                .build();

        PageModel<Item> result = itemDao.listOwnerItems(query);

        assertEquals(1, result.getTotalItems());
        assertEquals(active.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testListOwnerItemsPagination() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item3 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item1, itemType, location, "Kayak 1");
        insertVersion(em, item2, itemType, location, "Kayak 2");
        insertVersion(em, item3, itemType, location, "Kayak 3");
        em.flush();

        MyBoatsQueryModel query = MyBoatsQueryModel.builder()
                .ownerId(host.getId())
                .page(1)
                .pageSize(12)
                .build();

        PageModel<Item> result = itemDao.listOwnerItems(query);

        assertEquals(3, result.getTotalItems());
        assertEquals(3, result.getContent().size());
    }

    @Test
    public void testFindItemById() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();

        Optional<Item> found = itemDao.findItemById(item.getId());

        assertTrue(found.isPresent());
        assertEquals(item.getId(), found.get().getId());
    }

    @Test
    public void testFindImageById() {
        Image image = insertImage(em);
        em.flush();

        Optional<Image> found = itemDao.findImageById(image.getId());

        assertTrue(found.isPresent());
        assertEquals(image.getId(), found.get().getId());
    }

    @Test
    public void testGetVersionCount() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(
                em, item, itemType, location, "Version 1", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        insertVersion(
                em,
                item,
                itemType,
                location,
                "Version 2",
                BigDecimal.valueOf(150),
                4,
                200,
                2,
                LocalDateTime.now().plusDays(1));
        em.flush();

        int count = itemDao.getVersionCount(item.getId());

        assertEquals(2, count);
    }

    @Test
    public void testDeleteItem() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();
        int itemId = item.getId();

        Item managed = em.find(Item.class, itemId);
        itemDao.deleteItem(managed);
        em.flush();
        em.clear();

        assertNull(em.find(Item.class, itemId));
    }

    @Test
    public void testDeleteVersion() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        int versionId = v.getId();

        Version managed = em.find(Version.class, versionId);
        itemDao.deleteVersion(managed);
        em.flush();
        em.clear();

        assertNull(em.find(Version.class, versionId));
    }

    @Test
    public void testPersistItem() {
        Item item = new Item();
        item.setHost(host);
        item.setStatus(ItemStatusEnum.ACTIVE);
        item.setCreatedAt(LocalDateTime.now());

        Item persisted = itemDao.persistItem(item);
        assertNotNull(persisted.getId());
        em.flush();
        em.clear();

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

        Version persisted = itemDao.persistVersion(v);
        assertNotNull(persisted.getId());
        em.flush();
        em.clear();

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

        Availability persisted = itemDao.persistAvailability(availability);
        assertNotNull(persisted.getId());
        em.flush();
        em.clear();

        assertNotNull(em.find(Availability.class, persisted.getId()));
    }

    @Test
    public void testPersistImage() {
        Image image = new Image();
        image.setData(new byte[] {1, 2, 3});

        Image persisted = itemDao.persistImage(image);
        assertNotNull(persisted.getId());
        em.flush();
        em.clear();

        assertNotNull(em.find(Image.class, persisted.getId()));
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

        Media persisted = itemDao.persistMedia(media);
        assertNotNull(persisted.getId());
        em.flush();
        em.clear();

        assertNotNull(em.find(Media.class, persisted.getId()));
    }

    @Test
    public void testFindVersionByIdFound() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(
                em, item, itemType, location, "Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        em.flush();

        Optional<Version> result = itemDao.findVersionById(v.getId());

        assertTrue(result.isPresent());
        assertEquals(v.getId(), result.get().getId());
        assertEquals("Boat", result.get().getTitle());
    }

    @Test
    public void testRemoveVersionChildrenAvailabilities() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(
                em, item, itemType, location, "Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        em.flush();

        Version managed = em.find(Version.class, v.getId());
        Availability availability = insertAvailability(em, managed, WeekdayEnum.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        em.flush();
        em.clear();

        managed = em.find(Version.class, v.getId());
        itemDao.removeVersionChildren(managed);
        em.flush();
        em.clear();

        assertNull(em.find(Availability.class, availability.getId()));
    }

    @Test
    public void testRemoveVersionChildrenMedia() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(
                em, item, itemType, location, "Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        em.flush();

        Version managed = em.find(Version.class, v.getId());
        Availability availability =
                insertAvailability(em, managed, WeekdayEnum.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Image image = insertImage(em);
        Media media = insertMedia(em, managed, image, 0);
        em.flush();
        em.clear();

        managed = em.find(Version.class, v.getId());
        itemDao.removeVersionChildren(managed);
        em.flush();
        em.clear();

        assertNull(em.find(Availability.class, availability.getId()));
        assertNull(em.find(Media.class, media.getId()));
        assertNotNull(em.find(Image.class, image.getId()));
    }

    @Test
    public void testRemoveVersionChildrenRemoves() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        Version managed = em.find(Version.class, v.getId());
        Media media = insertMedia(em, managed, insertImage(em), 0);
        em.flush();

        itemDao.removeVersionChildren(managed);
        em.flush();
        em.clear();
        
        assertNull(em.find(Media.class, media.getId()));
    }

    @Test
    public void testRemoveVersionChildrenReuse() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version v = insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        Version managed = em.find(Version.class, v.getId());
        insertMedia(em, managed, insertImage(em), 0);
        em.flush();
        itemDao.removeVersionChildren(managed);
        em.flush();
        em.clear();

        Media media = insertMedia(em, managed, insertImage(em), 0);
        em.flush();

        assertNotNull(em.find(Media.class, media.getId()));
    }
}
