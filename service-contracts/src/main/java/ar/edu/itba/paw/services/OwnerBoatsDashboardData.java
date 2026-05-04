package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

@Getter
public final class OwnerBoatsDashboardData {

    private final List<Item> ownedItems;
    private final Map<Integer, Integer> publicationCoverImageIdsByItemId;
    private final Set<Integer> imageItemIds;
    private final List<OwnerReceivedBookingCard> receivedBookingCards;
    private final Page<ItemBooking> receivedBookingPage;
    private final Map<Integer, Boolean> publicationDeleteDeactivatesByItemId;
    private final Map<Integer, Boolean> publicationDeleteDisabledByItemId;

    public OwnerBoatsDashboardData(
            final List<Item> ownedItems,
            final Map<Integer, Integer> publicationCoverImageIdsByItemId,
            final Set<Integer> imageItemIds,
            final List<OwnerReceivedBookingCard> receivedBookingCards,
            final Page<ItemBooking> receivedBookingPage,
            final Map<Integer, Boolean> publicationDeleteDeactivatesByItemId,
            final Map<Integer, Boolean> publicationDeleteDisabledByItemId) {
        this.ownedItems = ownedItems;
        this.publicationCoverImageIdsByItemId = publicationCoverImageIdsByItemId;
        this.imageItemIds = imageItemIds;
        this.receivedBookingCards = receivedBookingCards;
        this.receivedBookingPage = receivedBookingPage;
        this.publicationDeleteDeactivatesByItemId = publicationDeleteDeactivatesByItemId;
        this.publicationDeleteDisabledByItemId = publicationDeleteDisabledByItemId;
    }
}
