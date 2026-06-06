package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.FavouriteDao;
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

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    @Test
    public void addFavouriteCreatesFavouriteForValidItem() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(ITEM_ID, OTHER_USER_ID, ItemStatusEnum.ACTIVE));

        assertTrue(favouriteService.addFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao).create(USER_ID, ITEM_ID);
    }

    @Test
    public void addFavouriteIsIdempotentForExistingFavourite() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(ITEM_ID, OTHER_USER_ID, ItemStatusEnum.ACTIVE));
        when(favouriteDao.create(USER_ID, ITEM_ID)).thenReturn(false);

        assertTrue(favouriteService.addFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao).create(USER_ID, ITEM_ID);
    }

    @Test
    public void addFavouriteRejectsOwnItem() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(ITEM_ID, USER_ID, ItemStatusEnum.ACTIVE));

        assertFalse(favouriteService.addFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao, never()).create(anyInt(), anyInt());
    }

    @Test
    public void addFavouriteRejectsDeletedItem() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(item(ITEM_ID, OTHER_USER_ID, ItemStatusEnum.DELETED));

        assertFalse(favouriteService.addFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao, never()).create(anyInt(), anyInt());
    }

    @Test
    public void addFavouriteRejectsMissingItem() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(null);

        assertFalse(favouriteService.addFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao, never()).create(anyInt(), anyInt());
    }

    @Test
    public void removeFavouriteReturnsTrueWhenFavouriteDoesNotExist() {
        when(favouriteDao.delete(USER_ID, ITEM_ID)).thenReturn(false);

        assertTrue(favouriteService.removeFavourite(USER_ID, ITEM_ID));

        verify(favouriteDao).delete(USER_ID, ITEM_ID);
    }

    private static Item item(final int itemId, final int hostId, final ItemStatusEnum status) {
        final Users host = new Users();
        host.setId(hostId);
        final Item item = new Item();
        item.setId(itemId);
        item.setHost(host);
        item.setStatus(status);
        return item;
    }
}
