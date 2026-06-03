package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.DetailDao;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DetailImplTest {

    private static final int ITEM_ID = 1;
    private static final int HOST_ID = 10;
    private static final int WRONG_HOST_ID = 20;
    private static final int REVIEW_PAGE = 0;

    @Mock
    private DetailDao detailDao;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private DetailImpl detailService;

    @Test
    public void detailReturnsItemWhenFound() {
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.of(detailItem(HOST_ID, true)));

        Item result = detailService.getItemDetail(ITEM_ID, REVIEW_PAGE);

        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    public void detailThrowsNotFoundWhenEmpty() {
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> detailService.getItemDetail(ITEM_ID, REVIEW_PAGE));
    }

    @Test
    public void detailThrowsNotFoundWhenNoVersion() {
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.of(detailItem(HOST_ID, false)));

        assertThrows(ItemNotFoundException.class, () -> detailService.getItemDetail(ITEM_ID, REVIEW_PAGE));
    }

    @Test
    public void detailWithHostReturnsItem() {
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.of(detailItem(HOST_ID, true)));

        Item result = detailService.getItemDetail(ITEM_ID, REVIEW_PAGE, HOST_ID);

        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    public void detailWithHostThrowsForbidden() {
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.of(detailItem(HOST_ID, true)));

        assertThrows(
                ForbiddenOperationException.class,
                () -> detailService.getItemDetail(ITEM_ID, REVIEW_PAGE, WRONG_HOST_ID));
    }

    @Test
    public void detailWithHostThrowsForbiddenNoHost() {
        Item item = Item.builder()
                .id(ITEM_ID)
                .host(null)
                .status(ItemStatusEnum.ACTIVE)
                .latestVersion(Version.builder().id(1).build())
                .build();
        when(detailDao.getItemDetail(ITEM_ID, REVIEW_PAGE)).thenReturn(Optional.of(item));

        assertThrows(
                ForbiddenOperationException.class, () -> detailService.getItemDetail(ITEM_ID, REVIEW_PAGE, HOST_ID));
    }

    private static Item detailItem(final int hostId, final boolean withVersion) {
        Users host = null;
        if (hostId >= 0) {
            host = new Users();
            host.setId(hostId);
        }
        Version version = withVersion ? Version.builder().id(1).build() : null;
        return Item.builder()
                .id(ITEM_ID)
                .host(host)
                .status(ItemStatusEnum.ACTIVE)
                .latestVersion(version)
                .build();
    }
}
