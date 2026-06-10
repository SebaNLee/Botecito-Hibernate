package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.insertItem;
import static ar.edu.itba.paw.persistence.TestUtils.insertItemType;
import static ar.edu.itba.paw.persistence.TestUtils.insertLocation;
import static ar.edu.itba.paw.persistence.TestUtils.insertUser;
import static ar.edu.itba.paw.persistence.TestUtils.insertVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Favourite;
import ar.edu.itba.paw.models.entity.FavouriteId;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Users;
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
public class FavouriteJpaDaoTest {

    @Autowired
    private FavouriteDao favouriteDao;

    @PersistenceContext
    private EntityManager em;

    private Users user;
    private Users host;
    private Location location;
    private ItemType itemType;

    @BeforeEach
    public void setup() {
        user = insertUser(em, "Fav", "User", "botecito.user@gmail.com");
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        em.flush();
    }

    @Test
    public void testCreate() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        boolean created = favouriteDao.create(user.getId(), item.getId());
        em.flush();
        em.clear();

        assertTrue(created);
        assertNotNull(em.find(Favourite.class, new FavouriteId(user.getId(), item.getId())));
    }

    @Test
    public void testCreateDuplicate() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        assertTrue(favouriteDao.create(user.getId(), item.getId()));
        assertFalse(favouriteDao.create(user.getId(), item.getId()));
    }

    @Test
    public void testDelete() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        favouriteDao.create(user.getId(), item.getId());

        boolean deleted = favouriteDao.delete(user.getId(), item.getId());

        em.flush();
        em.clear();

        assertTrue(deleted);
        assertNull(em.find(Favourite.class, new FavouriteId(user.getId(), item.getId())));
    }

    @Test
    public void testListFavourites() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item1, itemType, location, "Boat 1");
        insertVersion(em, item2, itemType, location, "Boat 2");
        em.flush();
        favouriteDao.create(user.getId(), item1.getId());
        favouriteDao.create(user.getId(), item2.getId());

        FavouritesQueryModel query = FavouritesQueryModel.builder()
                .userId(user.getId())
                .page(1)
                .pageSize(12)
                .build();

        PageModel<Item> result = favouriteDao.listFavourites(query);

        assertEquals(2, result.getTotalItems());
        assertEquals(2, result.getContent().size());
    }

    @Test
    public void testCountFavourites() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item1, itemType, location, "Boat 1");
        insertVersion(em, item2, itemType, location, "Boat 2");
        em.flush();
        favouriteDao.create(user.getId(), item1.getId());
        favouriteDao.create(user.getId(), item2.getId());

        FavouritesQueryModel query = FavouritesQueryModel.builder()
                .userId(user.getId())
                .page(1)
                .pageSize(12)
                .build();

        long count = favouriteDao.countFavourites(query);

        assertEquals(2, count);
    }
}
