package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.FavouritesQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.*;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
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
        assertTrue(created);
        em.flush();
        em.clear();

        assertNotNull(em.find(Favourite.class, new FavouriteId(user.getId(), item.getId())));
    }

    @Test
    public void testCreateDuplicate() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        insertFav(item, user);
        em.flush();

        assertFalse(favouriteDao.create(user.getId(), item.getId()));
    }

    @Test
    public void testDelete() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        insertFav(item, user);
        em.flush();

        boolean deleted = favouriteDao.delete(user.getId(), item.getId());
        assertTrue(deleted);
        em.flush();
        em.clear();

        assertNull(em.find(Favourite.class, new FavouriteId(user.getId(), item.getId())));
    }

    @Test
    public void testFindFavouriteItemIds() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item3 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item4 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item1, itemType, location, "Boat 1");
        insertVersion(em, item2, itemType, location, "Boat 2");
        insertVersion(em, item3, itemType, location, "Boat 3");
        insertVersion(em, item4, itemType, location, "Boat 4");
        em.flush();
        insertFav(item1, user);
        insertFav(item3, user);
        em.flush();

        Set<Integer> favIds = favouriteDao.findFavouriteItemIds(
                user.getId(), List.of(item1.getId(), item2.getId(), item3.getId(), item4.getId()));

        assertEquals(Set.of(item1.getId(), item3.getId()), favIds);
    }

    @Test
    public void testListFavourites() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item1, itemType, location, "Boat 1");
        insertVersion(em, item2, itemType, location, "Boat 2");
        em.flush();
        insertFav(item1, user);
        insertFav(item2, user);
        em.flush();

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
        insertFav(item1, user);
        insertFav(item2, user);
        em.flush();

        FavouritesQueryModel query = FavouritesQueryModel.builder()
                .userId(user.getId())
                .page(1)
                .pageSize(12)
                .build();

        long count = favouriteDao.countFavourites(query);

        assertEquals(2, count);
    }

    private Favourite insertFav(final Item item, final Users user) {
        Favourite fav = new Favourite();
        fav.setId(new FavouriteId(user.getId(), item.getId()));
        fav.setUser(user);
        fav.setItem(item);
        fav.setCreatedAt(LocalDateTime.now());
        em.persist(fav);
        return fav;
    }
}
