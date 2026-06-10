package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.DetailDao;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private static final int VIEWER_ID = 30;
    private static final int WRONG_HOST_ID = 20;
    private static final int REVIEW_PAGE = 0;

    @Mock
    private DetailDao detailDao;

    @Mock
    private BookingService bookingService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private FavouriteService favouriteService;

    @Mock
    private ReportService reportService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private DetailImpl detailService;

    @Test
    public void detailPageReturnsItemAndAvailability() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));
        when(availabilityService.buildAvailabilityData(any(), any(), any()))
                .thenReturn(new AvailabilityData(List.of(), List.of(), Map.of(), Map.of()));
        when(availabilityService.listingCalendarToday(any())).thenReturn(LocalDate.of(2026, 6, 10));
        when(availabilityService.listingCalendarMaxInclusive(any())).thenReturn(LocalDate.of(2026, 8, 31));
        when(favouriteService.isFavourite(VIEWER_ID, ITEM_ID)).thenReturn(true);
        when(reportService.hasReported(VIEWER_ID, ITEM_ID)).thenReturn(false);
        when(subscriptionService.isSubscribed(VIEWER_ID, HOST_ID)).thenReturn(true);

        var result = detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, VIEWER_ID);

        assertNotNull(result);
        assertEquals(ITEM_ID, result.getItem().getId());
        assertNotNull(result.getAvailabilityData());
        assertFalse(result.getFlags().isOwner());
        assertTrue(result.getFlags().isFavouriteItem());
        assertTrue(result.getFlags().isSubscribedToOwner());
    }

    @Test
    public void detailPageHostReturnsItem() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));
        when(availabilityService.buildAvailabilityData(any(), any(), any()))
                .thenReturn(new AvailabilityData(List.of(), List.of(), Map.of(), Map.of()));
        when(availabilityService.listingCalendarToday(any())).thenReturn(LocalDate.of(2026, 6, 10));
        when(availabilityService.listingCalendarMaxInclusive(any())).thenReturn(LocalDate.of(2026, 8, 31));

        var result = detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, HOST_ID, HOST_ID);

        assertEquals(ITEM_ID, result.getItem().getId());
        assertTrue(result.getFlags().isOwner());
        assertFalse(result.getFlags().isCanFavouriteItem());
    }

    @Test
    public void detailPageThrowsNotFoundWhenEmpty() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, null));
    }

    @Test
    public void detailPageWrongHostThrowsForbidden() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));

        assertThrows(
                ForbiddenOperationException.class,
                () -> detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, WRONG_HOST_ID, null));
    }

    private static Item detailItem(final int hostId, final boolean withVersion) {
        Version version = withVersion ? version(1) : null;
        return item(ITEM_ID, hostId >= 0 ? user(hostId) : null, ItemStatusEnum.ACTIVE, version);
    }
}
