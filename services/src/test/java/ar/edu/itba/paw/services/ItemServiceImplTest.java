package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImplTest {

    @InjectMocks
    private ItemServiceImpl itemService;

    @Mock
    private ItemDao itemDao;

    @Test
    public void testFindItemByIdWhenItemExists() {
        final Item item = new Item();
        item.setId(10);
        Mockito.when(itemDao.findItemById(10)).thenReturn(Optional.of(item));
        final Optional<Item> result = itemService.findItemById(10);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get().getId());
    }

    @Test
    public void testCreatePublicationWhenOwnerDoesNotExist() {
        final User createdUser = new User();
        createdUser.setId(1);
        createdUser.setGivenName("A");
        createdUser.setLastName("A");
        createdUser.setEmail("a@a.com");

        final Item createdItem = new Item();
        createdItem.setId(99);

        final ItemType itemType = new ItemType();
        itemType.setId(1);
        final LocationOption locationOption = new LocationOption();
        locationOption.setId(1);

        final ItemAvailability availability = new ItemAvailability();
        availability.setWeekday(DayOfWeek.MONDAY);
        availability.setStartTime(LocalTime.of(10, 0));
        availability.setEndTime(LocalTime.of(12, 0));

        Mockito.when(itemDao.findItemTypeById(1)).thenReturn(Optional.of(itemType));
        Mockito.when(itemDao.listLocationOptions()).thenReturn(List.of(locationOption));
        Mockito.when(itemDao.findUserByEmail("a@a.com")).thenReturn(Optional.empty());
        Mockito.when(itemDao.createUser("A", "A", "a@a.com", "es")).thenReturn(createdUser);
        Mockito.when(itemDao.createItem(
                        Mockito.eq(1),
                        Mockito.eq(1),
                        Mockito.eq("item-a"),
                        Mockito.eq("a"),
                        Mockito.eq(2000),
                        Mockito.eq(2),
                        Mockito.eq(BigDecimal.valueOf(100)),
                        Mockito.eq(1),
                        Mockito.eq(1),
                        Mockito.anyString()))
                .thenReturn(createdItem);
        final Item result = itemService.createPublication(
                "A",
                "A",
                "a@a.com",
                "pt",
                1,
                "item-a",
                "a",
                2000,
                2,
                BigDecimal.valueOf(100),
                1,
                1,
                List.of(availability));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(99, result.getId());
        Mockito.verify(itemDao).createUser("A", "A", "a@a.com", "es");
        Mockito.verify(itemDao).createItemAvailability(99, "MONDAY", "10:00", "12:00");
    }

    @Test
    public void testUpdatePublicationForOwnerWhenItemDoesNotBelongToOwnerReturnsFalse() {
        Mockito.when(itemDao.findItemByIdForOwner(10, 99)).thenReturn(Optional.empty());

        final boolean updated = itemService.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1, null);

        Assertions.assertFalse(updated);
        Mockito.verify(itemDao, Mockito.never()).snapshotBookingsForPublicationEdit(Mockito.anyInt());
        Mockito.verify(itemDao, Mockito.never())
                .updatePublicationForOwner(
                        Mockito.anyInt(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.anyInt());
    }

    @Test
    public void testUpdatePublicationForOwnerWhenSnapshotFailsStopsEdit() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(99);
        Mockito.when(itemDao.findItemByIdForOwner(10, 99)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.snapshotBookingsForPublicationEdit(10)).thenReturn(false);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> itemService.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1, null));

        Mockito.verify(itemDao, Mockito.never())
                .updatePublicationForOwner(
                        Mockito.anyInt(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.anyInt());
        Mockito.verify(itemDao, Mockito.never())
                .replacePrimaryImageForOwner(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    public void testUpdatePublicationForOwnerWhenUpdateFailsThrowsForRollback() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(99);
        Mockito.when(itemDao.findItemByIdForOwner(10, 99)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.snapshotBookingsForPublicationEdit(10)).thenReturn(true);
        Mockito.when(itemDao.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1))
                .thenReturn(false);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> itemService.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1, null));

        Mockito.verify(itemDao, Mockito.never())
                .replacePrimaryImageForOwner(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    public void testUpdatePublicationForOwnerWhenImageReplaceFailsThrowsForRollback() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(99);
        final byte[] imageData = new byte[] {1, 2, 3};
        Mockito.when(itemDao.findItemByIdForOwner(10, 99)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.snapshotBookingsForPublicationEdit(10)).thenReturn(true);
        Mockito.when(itemDao.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1))
                .thenReturn(true);
        Mockito.when(itemDao.replacePrimaryImageForOwner(10, 99, imageData)).thenReturn(null);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> itemService.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1, imageData));
    }

    @Test
    public void testUpdatePublicationForOwnerWhenDataIsValidAppliesImageInsideEdit() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(99);
        final byte[] imageData = new byte[] {1, 2, 3};
        Mockito.when(itemDao.findItemByIdForOwner(10, 99)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.snapshotBookingsForPublicationEdit(10)).thenReturn(true);
        Mockito.when(itemDao.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1))
                .thenReturn(true);
        Mockito.when(itemDao.replacePrimaryImageForOwner(10, 99, imageData)).thenReturn(7);

        final boolean updated =
                itemService.updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1, imageData);

        Assertions.assertTrue(updated);
        Mockito.verify(itemDao).snapshotBookingsForPublicationEdit(10);
        Mockito.verify(itemDao).updatePublicationForOwner(10, 99, "title", "description", 2000, 1, 1);
        Mockito.verify(itemDao).replacePrimaryImageForOwner(10, 99, imageData);
    }
}
