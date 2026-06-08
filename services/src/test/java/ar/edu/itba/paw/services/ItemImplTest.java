package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.persistence.ItemDao;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemImplTest {

    private static final int ITEM_ID = 1;
    private static final int IMAGE_ID = 10;
    private static final int USER_ID = 100;
    private static final int PAGE = 2;
    private static final int PAGE_SIZE = 10;
    private static final int OTHER_USER_ID = 200;

    @Mock
    private ItemDao itemDao;

    @InjectMocks
    private ItemImpl itemService;

    @Test
    public void listOwnerItemsReturnsResult() {
        PageModel<Item> expected = new PageModel<>(List.of(), 1, 12, 0L);
        when(itemDao.listOwnerItems(any())).thenReturn(expected);

        PageModel<Item> result = itemService.listOwnerItems(USER_ID, null, null, PAGE, PAGE_SIZE, null);

        assertNotNull(result);
        assertEquals(expected.getContent(), result.getContent());
    }

    @Test
    public void findItemByIdReturnsItem() {
        Item item = item(ITEM_ID);
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        Item result = itemService.findItemById(ITEM_ID);

        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    public void findImageByIdReturnsImage() {
        Image image = new Image();
        image.setId(IMAGE_ID);
        image.setData(new byte[] {1, 2, 3});
        when(itemDao.findImageById(IMAGE_ID)).thenReturn(Optional.of(image));

        Optional<Image> result = itemService.findImageById(IMAGE_ID);

        assertTrue(result.isPresent());
    }

    @Test
    public void userOwnsItemIdTrue() {
        Item item = item(ITEM_ID, user(USER_ID));
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        assertTrue(itemService.userOwnsItem(ITEM_ID, USER_ID));
    }

    @Test
    public void userOwnsItemItemTrue() {
        assertTrue(itemService.userOwnsItem(item(ITEM_ID, user(USER_ID)), USER_ID));
    }

    @Test
    public void requireOwnedItemReturnsItem() {
        Item item = item(ITEM_ID, user(USER_ID));
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        Item result = itemService.requireOwnedItem(ITEM_ID, USER_ID);

        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    public void requireOwnedItemNotOwnerThrows() {
        Item item = item(ITEM_ID, user(USER_ID));
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThrows(ForbiddenOperationException.class, () -> itemService.requireOwnedItem(ITEM_ID, OTHER_USER_ID));
    }

    @Test
    public void requireOwnedFullDataVersion() {
        Item item = item(ITEM_ID, user(USER_ID));
        var v = version(1);
        v.setAvailabilities(List.of());
        v.setMedia(List.of());
        item.setLatestVersion(v);
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        var result = itemService.requireOwnedFullData(ITEM_ID, USER_ID);

        assertNotNull(result);
    }

    @Test
    public void getVersionCountReturnsCount() {
        when(itemDao.getVersionCount(ITEM_ID)).thenReturn(5);

        assertEquals(5, itemService.getVersionCount(ITEM_ID));
    }

    @Test
    public void testDeleteItemSoft() {
        var item = item(ITEM_ID);
        item.setLatestVersion(version(1));
        assertDoesNotThrow(() -> itemService.deleteItem(item, true));
    }

    @Test
    public void testDeleteItemHard() {
        assertDoesNotThrow(() -> itemService.deleteItem(item(ITEM_ID), false));
    }

    @Test
    public void testCreate() {
        when(itemDao.persistItem(any())).thenAnswer(i -> i.getArgument(0));
        when(itemDao.persistVersion(any())).thenAnswer(i -> i.getArgument(0));

        var result = itemService.create(USER_ID, 1, "title", "desc", 100, 4, 50, 3, 1, List.of(), List.of());

        assertNotNull(result);
    }

    @Test
    public void testCreateNewVersion() {
        when(itemDao.persistVersion(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() ->
                itemService.createNewVersion(version(1), 1, "title", "desc", 100, 4, 50, 3, 1, List.of(), List.of()));
    }

    @Test
    public void testOverwriteVersion() {
        when(itemDao.findVersionById(1)).thenReturn(Optional.of(version(1)));

        assertDoesNotThrow(
                () -> itemService.overwriteVersion(1, 1, "title", "desc", 100, 4, 50, 3, 1, List.of(), List.of()));
    }
}
