package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.EditDao;
import ar.edu.itba.paw.persistence.ManageItemDao;
import ar.edu.itba.paw.persistence.ReportDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ManageItemServiceImplTest {

    private static final int ITEM_ID = 1;
    private static final int OWNER_ID = 10;

    @Mock
    private ManageItemDao manageItemDao;

    @Mock
    private ItemService itemService;

    @Mock
    private EditDao editDao;

    @Mock
    private BookingDao bookingDao;

    @Mock
    private ReportDao reportDao;

    @InjectMocks
    private ManageItemServiceImpl manageItemService;

    @Test
    public void setEnabledWithValidOwner() {
        when(itemService.requireOwnedItem(ITEM_ID, OWNER_ID))
                .thenReturn(Item.builder().id(ITEM_ID).build());

        assertDoesNotThrow(() -> manageItemService.setEnabled(ITEM_ID, OWNER_ID, true));
    }

    @Test
    public void setEnabledThrowsWhenNotOwner() {
        when(itemService.requireOwnedItem(ITEM_ID, OWNER_ID)).thenThrow(ForbiddenOperationException.class);

        assertThrows(ForbiddenOperationException.class, () -> manageItemService.setEnabled(ITEM_ID, OWNER_ID, true));
    }

    @Test
    public void deleteItemWithValidOwner() {
        when(itemService.requireOwnedItem(ITEM_ID, OWNER_ID))
                .thenReturn(Item.builder().id(ITEM_ID).build());

        assertDoesNotThrow(() -> manageItemService.deleteItem(ITEM_ID, OWNER_ID));
    }

    @Test
    public void deleteItemThrowsWhenNotOwner() {
        when(itemService.requireOwnedItem(ITEM_ID, OWNER_ID)).thenThrow(ForbiddenOperationException.class);

        assertThrows(ForbiddenOperationException.class, () -> manageItemService.deleteItem(ITEM_ID, OWNER_ID));
    }

    @Test
    public void deleteItemAsAdminWithValidItem() {
        when(itemService.findItemById(ITEM_ID))
                .thenReturn(Item.builder().id(ITEM_ID).build());

        assertDoesNotThrow(() -> manageItemService.deleteItemAsAdmin(ITEM_ID));
    }

    @Test
    public void deleteItemAsAdminThrowsWhenNotFound() {
        when(itemService.findItemById(ITEM_ID)).thenThrow(ItemNotFoundException.class);

        assertThrows(ItemNotFoundException.class, () -> manageItemService.deleteItemAsAdmin(ITEM_ID));
    }
}
