package ar.edu.itba.paw.models.dto;

import lombok.Value;

@Value
public class ReviewSummary {
    long totalReviews;
    double averageRating;
}
