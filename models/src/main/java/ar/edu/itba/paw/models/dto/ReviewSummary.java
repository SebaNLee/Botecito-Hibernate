package ar.edu.itba.paw.models.dto;

import lombok.Value;

@Value
public class ReviewSummary {

    public static final ReviewSummary EMPTY = new ReviewSummary(0L, 0.0);

    long totalReviews;
    double averageRating;
}
