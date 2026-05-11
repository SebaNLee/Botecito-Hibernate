package ar.edu.itba.paw.models.nuevo;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class DashboardReviewView {
    private final Map<Integer, PendingReviewActionModel> pendingItemReviewsByBookingId;
    private final Map<Integer, PendingReviewActionModel> pendingUserReviewsByBookingId;
    private final Map<Integer, ReviewModel> authoredItemReviewsByBookingId;
    private final Map<Integer, ReviewModel> authoredUserReviewsByBookingId;
}
