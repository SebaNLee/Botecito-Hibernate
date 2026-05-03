package ar.edu.itba.paw.services.dto;

import java.util.List;

public final class ItemReviewGroupView {
    private final int itemId;
    private final String itemTitle;
    private final List<ReceivedReviewView> reviews;

    public ItemReviewGroupView(final int itemId, final String itemTitle, final List<ReceivedReviewView> reviews) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.reviews = reviews == null ? List.of() : List.copyOf(reviews);
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public List<ReceivedReviewView> getReviews() {
        return reviews;
    }
}
