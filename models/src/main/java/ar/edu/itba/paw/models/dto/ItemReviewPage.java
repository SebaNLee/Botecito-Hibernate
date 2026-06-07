package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Review;
import lombok.Value;

@Value
public class ItemReviewPage {
    PageModel<Review> page;
    double averageRating;
}
