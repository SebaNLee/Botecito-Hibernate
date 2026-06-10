package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.DetailPageFlags;
import ar.edu.itba.paw.models.dto.ItemDetailPageData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
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
    private final FavouriteService favouriteService;
    private final ReportService reportService;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public ItemDetailPageData getItemDetailPage(final int itemId, final int reviewPage, final Integer viewerId) {
        return buildPageData(requireItemDetail(itemId, reviewPage), viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDetailPageData getItemDetailPage(
            final int itemId, final int reviewPage, final int hostId, final Integer viewerId) {
        final Item item = requireItemDetail(itemId, reviewPage);
        if (item.getHost() == null || item.getHost().getId() != hostId) {
            throw new ForbiddenOperationException();
        }
        return buildPageData(item, viewerId);
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

    private ItemDetailPageData buildPageData(final Item item, final Integer viewerId) {
        final Version version = item.getLatestVersion();
        final String timezone = version.getTimezone();
        final List<Availability> windows =
                version.getAvailabilities() == null ? List.of() : version.getAvailabilities();
        final List<Booking> bookings = item.getBookings() == null ? List.of() : item.getBookings();
        final AvailabilityData availabilityData =
                availabilityService.buildAvailabilityData(windows, bookings, timezone);
        final LocalDate listingCalendarToday = availabilityService.listingCalendarToday(timezone);
        final LocalDate listingCalendarMaxInclusive = availabilityService.listingCalendarMaxInclusive(timezone);
        return new ItemDetailPageData(
                item,
                availabilityData,
                listingCalendarToday,
                listingCalendarMaxInclusive,
                computeDetailPageFlags(item, viewerId));
    }

    private DetailPageFlags computeDetailPageFlags(final Item item, final Integer viewerId) {
        final Users itemOwner = item.getHost();
        final boolean isActive = item.getStatus() == ItemStatusEnum.ACTIVE;
        final Integer ownerId = itemOwner != null ? itemOwner.getId() : null;
        final boolean isOwner = viewerId != null && ownerId != null && ownerId.equals(viewerId);
        final boolean canFavouriteItem = ownerId == null || viewerId == null || !ownerId.equals(viewerId);
        final boolean favouriteItem =
                canFavouriteItem && viewerId != null && favouriteService.isFavourite(viewerId, item.getId());
        final boolean alreadyReported =
                viewerId != null && isActive && !isOwner && reportService.hasReported(viewerId, item.getId());
        final boolean canSubscribeToOwner = viewerId != null && ownerId != null && !isOwner;
        final boolean subscribedToOwner;
        if (viewerId != null && ownerId != null && !isOwner) {
            subscribedToOwner = subscriptionService.isSubscribed(viewerId, ownerId);
        } else {
            subscribedToOwner = false;
        }
        return new DetailPageFlags(
                isOwner, canFavouriteItem, favouriteItem, alreadyReported, canSubscribeToOwner, subscribedToOwner);
    }
}
