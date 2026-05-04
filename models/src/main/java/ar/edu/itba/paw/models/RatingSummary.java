package ar.edu.itba.paw.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RatingSummary {
    private final double averageRating;
    private final int totalReviews;

    public static RatingSummary empty() {
        return new RatingSummary(0.0, 0);
    }

    public boolean hasReviews() {
        return totalReviews > 0;
    }
}
