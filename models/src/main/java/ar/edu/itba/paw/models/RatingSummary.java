package ar.edu.itba.paw.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummary {
    private double averageRating;
    private int totalReviews;

    public boolean hasReviews() {
        return totalReviews > 0;
    }

    public boolean getHasReviews() {
        return hasReviews();
    }
}
