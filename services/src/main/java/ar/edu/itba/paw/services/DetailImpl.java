package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.ItemDetailPageData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.DetailDao;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetailImpl implements DetailService {

    private final DetailDao detailDao;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final AvailabilityService availabilityService;

    @Override
    @Transactional(readOnly = true)
    public ItemDetailPageData getItemDetailPage(final int itemId, final int reviewPage) {
        return buildPageData(requireItemDetail(itemId, reviewPage));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDetailPageData getItemDetailPage(final int itemId, final int reviewPage, final int hostId) {
        final Item item = requireItemDetail(itemId, reviewPage);
        if (item.getHost() == null || item.getHost().getId() != hostId) {
            throw new ForbiddenOperationException();
        }
        return buildPageData(item);
    }

    private Item requireItemDetail(final int itemId, final int reviewPage) {
        final Item item = detailDao.getItemDetail(itemId).orElseThrow(ItemNotFoundException::new);
        if (item.getLatestVersion() == null) {
            throw new ItemNotFoundException();
        }

        reviewService.attachItemReviews(item, itemId, reviewPage);
        item.setBookings(bookingService.getUpcomingBookings(item));

        return item;
    }

    private ItemDetailPageData buildPageData(final Item item) {
        final Version version = item.getLatestVersion();
        final String timezone = version.getTimezone();
        final List<Availability> windows =
                version.getAvailabilities() == null ? List.of() : version.getAvailabilities();
        final List<Booking> bookings = item.getBookings() == null ? List.of() : item.getBookings();
        final AvailabilityData availabilityData =
                availabilityService.buildAvailabilityData(windows, bookings, timezone);
        final LocalDate listingCalendarToday = availabilityService.listingCalendarToday(timezone);
        final LocalDate listingCalendarMaxInclusive = availabilityService.listingCalendarMaxInclusive(timezone);
        return new ItemDetailPageData(item, availabilityData, listingCalendarToday, listingCalendarMaxInclusive);
    }
}
