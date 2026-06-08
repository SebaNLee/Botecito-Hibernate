package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
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

        assertNull(em.find(Version.class, versionId));
    }
}
