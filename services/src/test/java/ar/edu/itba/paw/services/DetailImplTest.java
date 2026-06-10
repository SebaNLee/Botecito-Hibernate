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

    @InjectMocks
    private DetailImpl detailService;

    @Test
    public void detailPageReturnsItemAndAvailability() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));
        when(availabilityService.buildAvailabilityData(any(), any(), any()))
                .thenReturn(new AvailabilityData(List.of(), List.of(), Map.of(), Map.of()));
        when(availabilityService.listingCalendarToday(any())).thenReturn(LocalDate.of(2026, 6, 10));
        when(availabilityService.listingCalendarMaxInclusive(any())).thenReturn(LocalDate.of(2026, 8, 31));

        var result = detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE);

        assertNotNull(result);
        assertEquals(ITEM_ID, result.getItem().getId());
        assertNotNull(result.getAvailabilityData());
    }

    @Test
    public void detailPageHostReturnsItem() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));
        when(availabilityService.buildAvailabilityData(any(), any(), any()))
                .thenReturn(new AvailabilityData(List.of(), List.of(), Map.of(), Map.of()));
        when(availabilityService.listingCalendarToday(any())).thenReturn(LocalDate.of(2026, 6, 10));
        when(availabilityService.listingCalendarMaxInclusive(any())).thenReturn(LocalDate.of(2026, 8, 31));

        var result = detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, HOST_ID);

        assertEquals(ITEM_ID, result.getItem().getId());
    }

    @Test
    public void detailPageThrowsNotFoundWhenEmpty() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE));
    }

    @Test
    public void detailPageWrongHostThrowsForbidden() {
        when(detailDao.getItemDetail(ITEM_ID)).thenReturn(Optional.of(detailItem(HOST_ID, true)));

        assertThrows(
                ForbiddenOperationException.class,
                () -> detailService.getItemDetailPage(ITEM_ID, REVIEW_PAGE, WRONG_HOST_ID));
    }

    private static Item detailItem(final int hostId, final boolean withVersion) {
        Version version = withVersion ? version(1) : null;
        return item(ITEM_ID, hostId >= 0 ? user(hostId) : null, ItemStatusEnum.ACTIVE, version);
    }
}
