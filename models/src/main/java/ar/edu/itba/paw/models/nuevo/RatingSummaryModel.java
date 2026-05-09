package ar.edu.itba.paw.models.nuevo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RatingSummaryModel {
    private final Double averageRating;
    private final long totalReviews;

    public static RatingSummaryModel empty() {
        return new RatingSummaryModel(0.0, 0L);
    }

    public boolean hasReviews() {
        return totalReviews > 0;
    }
}
