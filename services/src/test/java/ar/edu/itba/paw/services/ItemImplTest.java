package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Users;
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

    @Mock
    private ItemDao itemDao;

    @InjectMocks
    private ItemImpl itemService;

    @Test
    public void findItemByIdReturnsItem() {
        Item item = Item.builder().id(ITEM_ID).build();
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
    public void userOwnsItemByIdReturnsTrue() {
        Users host = new Users();
        host.setId(USER_ID);
        Item item = Item.builder().id(ITEM_ID).host(host).build();
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        assertTrue(itemService.userOwnsItem(ITEM_ID, USER_ID));
    }

    @Test
    public void listOwnerItemsReturnsResult() {
        SearchResult<Item> expected = new SearchResult<>(List.of(), 0L);
        when(itemDao.listOwnerItems(any())).thenReturn(expected);

        SearchResult<Item> result = itemService.listOwnerItems(USER_ID, null, null, null, PAGE, PAGE_SIZE, null);

        assertNotNull(result);
        assertEquals(expected.getPageElements(), result.getPageElements());
    }
}
