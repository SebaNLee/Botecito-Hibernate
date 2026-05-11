package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class OwnerBoatsDashboardData {

    private final List<Item> ownedItems;
    private final Map<Integer, Integer> publicationCoverImageIdsByItemId;
    private final Set<Integer> imageItemIds;
    private final List<OwnerReceivedBookingCard> receivedBookingCards;
    private final Page<ItemBooking> receivedBookingPage;
    private final Map<Integer, Boolean> publicationDeleteDeactivatesByItemId;
    private final Map<Integer, Boolean> publicationDeleteDisabledByItemId;
    private final Map<Integer, Boolean> receivedCanReviewByBookingId;
}
