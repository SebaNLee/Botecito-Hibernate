package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ReviewService.PendingReviewAction;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public final class MarketplaceItemView {
    private final Item item;
    private final boolean isOwner;
    private final Item displayItem;
    private final ItemSnapshot selectedSnapshot;
    private final boolean hideListingLiveVersionNavigation;
    private final boolean listingInactiveNotice;
    private final List<ItemSnapshot> guestSnapshots;
    private final List<ItemSnapshot> hostSnapshots;
    private final User itemOwner;
    private final ItemType itemType;
    private final RatingSummary itemRatingSummary;
    private final List<Review> itemReviews;
    private final Map<Integer, String> reviewAuthorNames;
    private final Integer coverImageId;
    private final List<Integer> galleryImageIds;
    private final boolean useSnapshotCoverImage;
    private final String ownerInitial;
    private final PendingReviewAction pendingItemReviewAction;
    private final AvailabilityPickerData availability;
    private final String resolvedDate;
    private final String resolvedStartTime;
    private final String resolvedEndTime;

    public MarketplaceItemView(
            final Item item,
            final boolean isOwner,
            final Item displayItem,
            final ItemSnapshot selectedSnapshot,
            final boolean hideListingLiveVersionNavigation,
            final boolean listingInactiveNotice,
            final List<ItemSnapshot> guestSnapshots,
            final List<ItemSnapshot> hostSnapshots,
            final User itemOwner,
            final ItemType itemType,
            final RatingSummary itemRatingSummary,
            final List<Review> itemReviews,
            final Map<Integer, String> reviewAuthorNames,
            final Integer coverImageId,
            final List<Integer> galleryImageIds,
            final boolean useSnapshotCoverImage,
            final String ownerInitial,
            final PendingReviewAction pendingItemReviewAction,
            final AvailabilityPickerData availability,
            final String resolvedDate,
            final String resolvedStartTime,
            final String resolvedEndTime) {
        this.item = item;
        this.isOwner = isOwner;
        this.displayItem = displayItem;
        this.selectedSnapshot = selectedSnapshot;
        this.hideListingLiveVersionNavigation = hideListingLiveVersionNavigation;
        this.listingInactiveNotice = listingInactiveNotice;
        this.guestSnapshots = guestSnapshots == null ? List.of() : List.copyOf(guestSnapshots);
        this.hostSnapshots = hostSnapshots == null ? List.of() : List.copyOf(hostSnapshots);
        this.itemOwner = itemOwner;
        this.itemType = itemType;
        this.itemRatingSummary = itemRatingSummary;
        this.itemReviews = itemReviews == null ? List.of() : List.copyOf(itemReviews);
        this.reviewAuthorNames = reviewAuthorNames == null ? Map.of() : Map.copyOf(reviewAuthorNames);
        this.coverImageId = coverImageId;
        this.galleryImageIds = galleryImageIds == null ? List.of() : List.copyOf(galleryImageIds);
        this.useSnapshotCoverImage = useSnapshotCoverImage;
        this.ownerInitial = ownerInitial;
        this.pendingItemReviewAction = pendingItemReviewAction;
        this.availability = availability;
        this.resolvedDate = resolvedDate;
        this.resolvedStartTime = resolvedStartTime;
        this.resolvedEndTime = resolvedEndTime;
    }
}
