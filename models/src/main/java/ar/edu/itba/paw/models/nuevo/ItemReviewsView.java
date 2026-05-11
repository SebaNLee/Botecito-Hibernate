package ar.edu.itba.paw.models.nuevo;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ItemReviewsView {
    private final RatingSummaryModel ratingSummary;
    private final List<ReviewModel> reviews;
    private final Map<Integer, String> authorNamesBySenderId;
    private final PendingReviewActionModel pendingViewerAction;
}
