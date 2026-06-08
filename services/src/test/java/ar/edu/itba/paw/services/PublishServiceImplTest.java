package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.persistence.PublishDao;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishServiceImplTest {

    private static final int OWNER_ID = 1;
    private static final int ITEM_ID = 10;
    private static final int TYPE_ID = 5;
    private static final String TITLE = "Test Item";
    private static final String DESCRIPTION = "Test Description";
    private static final int PRICE = 1000;
    private static final int CAPACITY = 4;
    private static final int WEIGHT = 50;
    private static final int DIFFICULTY = 3;
    private static final int LOCATION_ID = 2;

    @Mock
    private PublishDao publishDao;

    @Mock
    private MailService mailService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ItemService itemService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private PublishServiceImpl publishService;

    @Test
    public void createWorksOk() {
        Item item = item(1);
        when(publishDao.persistItem(any())).thenReturn(item);
        when(publishDao.persistVersion(any())).thenReturn(version(1, item));

        assertDoesNotThrow(() -> publishService.create(
                OWNER_ID,
                TYPE_ID,
                TITLE,
                DESCRIPTION,
                PRICE,
                CAPACITY,
                WEIGHT,
                DIFFICULTY,
                LOCATION_ID,
                List.of(),
                List.of()));
    }

    @Test
    public void createExceptionWhenIncomplete() {
        assertThrows(
                NullPointerException.class,
                () -> publishService.create(
                        OWNER_ID,
                        TYPE_ID,
                        TITLE,
                        DESCRIPTION,
                        PRICE,
                        CAPACITY,
                        WEIGHT,
                        null,
                        LOCATION_ID,
                        List.of(),
                        List.of()));
    }

    @Test
    public void editReturnsFalseWhenNoChanges() {
        when(itemService.requireOwnedFullData(ITEM_ID, OWNER_ID)).thenReturn(matchingVersion());

        boolean result = publishService.edit(
                ITEM_ID,
                OWNER_ID,
                TYPE_ID,
                TITLE,
                DESCRIPTION,
                PRICE,
                CAPACITY,
                WEIGHT,
                DIFFICULTY,
                LOCATION_ID,
                List.of(),
                List.of());

        assertFalse(result);
    }

    @Test
    public void editReturnsTrueWhenChangesExist() {
        Version current = matchingVersion();
        current.setTitle("Old Title");
        when(itemService.requireOwnedFullData(ITEM_ID, OWNER_ID)).thenReturn(current);
        when(bookingService.itemHasBookings(ITEM_ID)).thenReturn(true);
        when(publishDao.persistVersion(any())).thenReturn(version(99));

        boolean result = publishService.edit(
                ITEM_ID,
                OWNER_ID,
                TYPE_ID,
                TITLE,
                DESCRIPTION,
                PRICE,
                CAPACITY,
                WEIGHT,
                DIFFICULTY,
                LOCATION_ID,
                List.of(),
                List.of());

        assertTrue(result);
    }

    @Test
    public void editThrowsWhenNotOwner() {
        when(itemService.requireOwnedFullData(ITEM_ID, OWNER_ID)).thenThrow(ForbiddenOperationException.class);

        assertThrows(
                ForbiddenOperationException.class,
                () -> publishService.edit(
                        ITEM_ID,
                        OWNER_ID,
                        TYPE_ID,
                        TITLE,
                        DESCRIPTION,
                        PRICE,
                        CAPACITY,
                        WEIGHT,
                        DIFFICULTY,
                        LOCATION_ID,
                        List.of(),
                        List.of()));
    }

    private static Version matchingVersion() {
        ItemType type = new ItemType();
        type.setId(TYPE_ID);
        Location location = new Location();
        location.setId(LOCATION_ID);
        return Version.builder()
                .id(99)
                .type(type)
                .title(TITLE)
                .description(DESCRIPTION)
                .price(BigDecimal.valueOf(PRICE))
                .capacity(CAPACITY)
                .weight(WEIGHT)
                .difficulty(DIFFICULTY)
                .location(location)
                .build();
    }
}
