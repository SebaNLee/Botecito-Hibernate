package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetailPageFlagsFactory {

    private final FavouriteService favouriteService;
    private final ReportService reportService;
    private final SubscriptionService subscriptionService;

    public DetailPageFlags compute(final Item item, final BotecitoUserDetails viewer) {
        final Users itemOwner = item.getHost();
        final boolean isActive = item.getStatus() == ItemStatusEnum.ACTIVE;
        final Integer viewerId = viewer != null ? viewer.getId() : null;
        final Integer ownerId = itemOwner != null ? itemOwner.getId() : null;
        final boolean isOwner = viewerId != null && ownerId != null && ownerId.equals(viewerId);
        final boolean canFavouriteItem = ownerId == null || viewerId == null || !ownerId.equals(viewerId);
        final boolean favouriteItem =
                canFavouriteItem && viewerId != null && favouriteService.isFavourite(viewerId, item.getId());
        final boolean alreadyReported =
                viewerId != null && isActive && !isOwner && reportService.hasReported(viewerId, item.getId());
        final boolean subscribedToOwner =
                ownerId != null && !isOwner && viewerId != null && subscriptionService.isSubscribed(viewerId, ownerId);
        return new DetailPageFlags(favouriteItem, alreadyReported, subscribedToOwner);
    }
}
