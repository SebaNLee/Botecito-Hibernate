package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.persistence.FavouriteDao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FavouriteServiceImplTest {

    private static final int USER_ID = 1;
    private static final int OTHER_USER_ID = 2;
    private static final int ITEM_ID = 10;

    @Mock
    private FavouriteDao favouriteDao;

    @Mock
    private ItemService itemService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    @Test
    public void testAdd() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(OTHER_USER_ID, ItemStatusEnum.ACTIVE));

        assertTrue(favouriteService.addFavourite(USER_ID, ITEM_ID));
    }

    @Test
    public void testAddOwn() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(USER_ID, ItemStatusEnum.ACTIVE));

        assertFalse(favouriteService.addFavourite(USER_ID, ITEM_ID));
    }

    @Test
    public void testRemove() {
        assertTrue(favouriteService.removeFavourite(USER_ID, ITEM_ID));
    }

    @Test
    public void testIsFav() {
        when(favouriteDao.exists(USER_ID, ITEM_ID)).thenReturn(true);

        assertTrue(favouriteService.isFavourite(USER_ID, ITEM_ID));
    }

    @Test
    public void testListFav() {
        var searchResult = new PageModel<Item>(List.of(), 1, 12, 0);
        when(favouriteDao.listFavourites(any())).thenReturn(searchResult);

        var result = favouriteService.listFavourites(USER_ID, null, 1, 10, null);

        assertNotNull(result);
        assertEquals(0, result.getTotalItems());
    }

    private static Item item(final int hostId, final ItemStatusEnum status) {
        return TestUtils.item(ITEM_ID, user(hostId), status);
    }
}
