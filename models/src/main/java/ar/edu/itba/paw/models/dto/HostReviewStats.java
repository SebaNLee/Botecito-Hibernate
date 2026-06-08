package ar.edu.itba.paw.models.dto;

import java.util.Optional;
import lombok.Value;

@Value
public class HostReviewStats {
    long totalReviews;
    Optional<Double> averageRating;
}
