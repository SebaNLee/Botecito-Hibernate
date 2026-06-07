package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
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
    private ItemService itemService;

    @Mock
    private BookingService bookingService;

    @Mock
    private ReportService reportService;

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
}
