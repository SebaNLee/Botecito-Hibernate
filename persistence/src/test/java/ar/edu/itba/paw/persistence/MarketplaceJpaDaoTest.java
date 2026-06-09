package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.MarketplaceQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.*;
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
public class MarketplaceJpaDaoTest {

    @Autowired
    private MarketplaceDao marketplaceDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Location location;
    private Location otherLocation;
    private ItemType itemType;
    private ItemType otherItemType;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        otherLocation = insertLocation(em, "Los Castores", "los-castores");
        itemType = insertItemType(em, "Kayak", "kayak");
        otherItemType = insertItemType(em, "Paddle", "paddle");
        em.flush();
    }

    @Test
    public void testSearchMarketplace() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        MarketplaceQueryModel query = new MarketplaceQueryModel();
        query.setPage(1);
        query.setPageSize(12);
        query.setSortBy("newest");

        PageModel<Item> result = marketplaceDao.searchMarketplace(query);

        assertEquals(1, result.getTotalItems());
        assertEquals(item.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testSearchMarketplaceFiltersByLocation() {
        Item itemA = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item itemB = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, itemA, itemType, location, "Boat 1");
        insertVersion(em, itemB, itemType, otherLocation, "Boat 2");
        em.flush();

        MarketplaceQueryModel query = new MarketplaceQueryModel();
        query.setLocationSlug("portezuelo");
        query.setPage(1);
        query.setPageSize(12);
        query.setSortBy("newest");

        PageModel<Item> result = marketplaceDao.searchMarketplace(query);

        assertEquals(1, result.getTotalItems());
        assertEquals(itemA.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testSearchMarketplaceFiltersByType() {
        Item kayakItem = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item paddleItem = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, kayakItem, itemType, location, "Kayak");
        insertVersion(em, paddleItem, otherItemType, location, "Paddle");
        em.flush();

        MarketplaceQueryModel query = new MarketplaceQueryModel();
        query.setItemTypeSlug("paddle");
        query.setPage(1);
        query.setPageSize(12);
        query.setSortBy("newest");

        PageModel<Item> result = marketplaceDao.searchMarketplace(query);

        assertEquals(1, result.getTotalItems());
        assertEquals(paddleItem.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testSearchMarketplacePagination() {
        for (int i = 1; i <= 14; i++) {
            Item it = insertItem(em, host, ItemStatusEnum.ACTIVE);
            insertVersion(em, it, itemType, location, "Boat " + i);
        }
        em.flush();

        MarketplaceQueryModel query = new MarketplaceQueryModel();
        query.setPage(1);
        query.setPageSize(12);
        query.setSortBy("newest");

        PageModel<Item> result = marketplaceDao.searchMarketplace(query);

        assertEquals(14, result.getTotalItems());
        assertEquals(12, result.getContent().size());
    }
}
