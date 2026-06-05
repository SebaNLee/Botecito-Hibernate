package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static ar.edu.itba.paw.persistence.TestUtils.*;

import ar.edu.itba.paw.models.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class ManageItemJpaDaoTest {

    @Autowired
    private ManageItemDao manageItemDao;

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
    public void testFindItemById() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();

        Optional<Item> found = manageItemDao.findItemById(item.getId());

        assertTrue(found.isPresent());
        assertEquals(item.getId(), found.get().getId());
    }

    @Test
    public void testCountVersionsByItemId() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Version 1", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
        insertVersion(em, item, itemType, location, "Version 2", BigDecimal.valueOf(150), 4, 200, 2, LocalDateTime.now().plusDays(1));
        em.flush();

        int count = manageItemDao.countVersionsByItemId(item.getId());

        assertEquals(2, count);
    }

    @Test
    public void testDeleteItem() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();
        int itemId = item.getId();

        Item managed = em.find(Item.class, itemId);
        manageItemDao.deleteItem(managed);
        em.flush();

        assertNull(em.find(Item.class, itemId));
    }

    @Test
    public void testDeleteVersion() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        int versionId = version.getId();

        manageItemDao.deleteVersion(versionId);
        em.flush();

        assertNull(em.find(Version.class, versionId));
    }

    @Test
    public void testFindLatestVersionIdByItemId() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Old Boat", BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now().minusDays(10));
        Version newest = insertVersion(em, item, itemType, location, "New Boat", BigDecimal.valueOf(150), 4, 200, 2, LocalDateTime.now());
        em.flush();

        Optional<Integer> latestId = manageItemDao.findLatestVersionIdByItemId(item.getId());

        assertTrue(latestId.isPresent());
        assertEquals(newest.getId(), latestId.get().intValue());
    }
}
