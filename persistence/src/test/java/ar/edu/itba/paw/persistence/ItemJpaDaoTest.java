package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.*;
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

        SearchResult<Item> result = itemDao.listOwnerItems(query);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getPageElements().size());
        assertEquals(item.getId(), result.getPageElements().get(0).getId());
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

        SearchResult<Item> result = itemDao.listOwnerItems(query);

        assertEquals(1, result.getTotalCount());
        assertEquals(active.getId(), result.getPageElements().get(0).getId());
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

        SearchResult<Item> result = itemDao.listOwnerItems(query);

        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getPageElements().size());
    }

    @Test
    public void testFindItemById() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();

        Optional<Item> found = itemDao.findItemById(item.getId());

        assertTrue(found.isPresent());
        assertEquals(item.getId(), found.get().getId());
    }
}
