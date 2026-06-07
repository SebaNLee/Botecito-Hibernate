package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.DetailPageFlags;
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
        final boolean isOwner = viewer != null && itemOwner != null && itemOwner.getId() == viewer.getId();
        final boolean canFavouriteItem = itemOwner == null || viewer == null || itemOwner.getId() != viewer.getId();
        final boolean favouriteItem =
                canFavouriteItem && viewer != null && favouriteService.isFavourite(viewer.getId(), item.getId());
        final boolean canReport = viewer != null && isActive && !isOwner;
        final boolean alreadyReported = canReport && reportService.hasReported(viewer.getId(), item.getId());
        final boolean canSubscribeToOwner = itemOwner != null && !isOwner;
        final boolean subscribedToOwner = canSubscribeToOwner
                && viewer != null
                && subscriptionService.isSubscribed(viewer.getId(), itemOwner.getId());
        return new DetailPageFlags(favouriteItem, alreadyReported, subscribedToOwner);
    }
}
