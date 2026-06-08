package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
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
    public void userOwnsItemByIdReturnsTrue() {
        Item item = item(ITEM_ID, user(USER_ID));
        when(itemDao.findItemById(ITEM_ID)).thenReturn(Optional.of(item));

        assertTrue(itemService.userOwnsItem(ITEM_ID, USER_ID));
    }

    @Test
    public void listOwnerItemsReturnsResult() {
        PageModel<Item> expected = new PageModel<>(List.of(), 1, 12, 0L);
        when(itemDao.listOwnerItems(any())).thenReturn(expected);

        PageModel<Item> result = itemService.listOwnerItems(USER_ID, null, null, PAGE, PAGE_SIZE, null);

        assertNotNull(result);
        assertEquals(expected.getContent(), result.getContent());
    }
}
