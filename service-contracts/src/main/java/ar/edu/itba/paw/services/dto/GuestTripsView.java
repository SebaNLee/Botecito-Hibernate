package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.services.Page;
import java.util.Map;
import java.util.Set;

public final class GuestTripsView {
    private final Page<SentBookingView> sentBookingPage;
    private final Set<Integer> itemIdsForImageUrls;
    private final Map<Integer, PendingReviewView> pendingGuestItemReviewsByBookingId;
    private final Map<Integer, AuthoredItemReviewSummaryView> authoredItemReviewsByBookingId;

    public GuestTripsView(
            final Page<SentBookingView> sentBookingPage,
            final Set<Integer> itemIdsForImageUrls,
            final Map<Integer, PendingReviewView> pendingGuestItemReviewsByBookingId,
            final Map<Integer, AuthoredItemReviewSummaryView> authoredItemReviewsByBookingId) {
        this.sentBookingPage = sentBookingPage;
        this.itemIdsForImageUrls = itemIdsForImageUrls == null ? Set.of() : Set.copyOf(itemIdsForImageUrls);
        this.pendingGuestItemReviewsByBookingId =
                pendingGuestItemReviewsByBookingId == null ? Map.of() : Map.copyOf(pendingGuestItemReviewsByBookingId);
        this.authoredItemReviewsByBookingId =
                authoredItemReviewsByBookingId == null ? Map.of() : Map.copyOf(authoredItemReviewsByBookingId);
    }

    public Page<SentBookingView> getSentBookingPage() {
        return sentBookingPage;
    }

    public Set<Integer> getItemIdsForImageUrls() {
        return itemIdsForImageUrls;
    }

    public Map<Integer, PendingReviewView> getPendingGuestItemReviewsByBookingId() {
        return pendingGuestItemReviewsByBookingId;
    }

    public Map<Integer, AuthoredItemReviewSummaryView> getAuthoredItemReviewsByBookingId() {
        return authoredItemReviewsByBookingId;
    }
}
