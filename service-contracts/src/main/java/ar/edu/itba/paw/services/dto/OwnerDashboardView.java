package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.services.Page;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OwnerDashboardView {
    private final List<Item> ownedItems;
    private final Map<Integer, Integer> coverImageIdsByItemId;
    private final Set<Integer> itemIdsForImageUrls;
    private final Map<Integer, Boolean> deleteDeactivatesByItemId;
    private final Map<Integer, Boolean> deleteDisabledByItemId;
    private final Page<ReceivedBookingView> receivedBookingPage;
    private final Map<Integer, PendingReviewView> pendingOwnerUserReviewsByBookingId;
    private final Map<Integer, AuthoredItemReviewSummaryView> authoredUserReviewsByBookingId;

    public OwnerDashboardView(
            final List<Item> ownedItems,
            final Map<Integer, Integer> coverImageIdsByItemId,
            final Set<Integer> itemIdsForImageUrls,
            final Map<Integer, Boolean> deleteDeactivatesByItemId,
            final Map<Integer, Boolean> deleteDisabledByItemId,
            final Page<ReceivedBookingView> receivedBookingPage,
            final Map<Integer, PendingReviewView> pendingOwnerUserReviewsByBookingId,
            final Map<Integer, AuthoredItemReviewSummaryView> authoredUserReviewsByBookingId) {
        this.ownedItems = ownedItems == null ? List.of() : List.copyOf(ownedItems);
        this.coverImageIdsByItemId = coverImageIdsByItemId == null ? Map.of() : Map.copyOf(coverImageIdsByItemId);
        this.itemIdsForImageUrls = itemIdsForImageUrls == null ? Set.of() : Set.copyOf(itemIdsForImageUrls);
        this.deleteDeactivatesByItemId =
                deleteDeactivatesByItemId == null ? Map.of() : Map.copyOf(deleteDeactivatesByItemId);
        this.deleteDisabledByItemId = deleteDisabledByItemId == null ? Map.of() : Map.copyOf(deleteDisabledByItemId);
        this.receivedBookingPage = receivedBookingPage;
        this.pendingOwnerUserReviewsByBookingId =
                pendingOwnerUserReviewsByBookingId == null ? Map.of() : Map.copyOf(pendingOwnerUserReviewsByBookingId);
        this.authoredUserReviewsByBookingId =
                authoredUserReviewsByBookingId == null ? Map.of() : Map.copyOf(authoredUserReviewsByBookingId);
    }

    public List<Item> getOwnedItems() {
        return ownedItems;
    }

    public Map<Integer, Integer> getCoverImageIdsByItemId() {
        return coverImageIdsByItemId;
    }

    public Set<Integer> getItemIdsForImageUrls() {
        return itemIdsForImageUrls;
    }

    public Map<Integer, Boolean> getDeleteDeactivatesByItemId() {
        return deleteDeactivatesByItemId;
    }

    public Map<Integer, Boolean> getDeleteDisabledByItemId() {
        return deleteDisabledByItemId;
    }

    public Page<ReceivedBookingView> getReceivedBookingPage() {
        return receivedBookingPage;
    }

    public Map<Integer, PendingReviewView> getPendingOwnerUserReviewsByBookingId() {
        return pendingOwnerUserReviewsByBookingId;
    }

    public Map<Integer, AuthoredItemReviewSummaryView> getAuthoredUserReviewsByBookingId() {
        return authoredUserReviewsByBookingId;
    }
}
