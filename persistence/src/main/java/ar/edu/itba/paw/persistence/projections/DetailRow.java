package ar.edu.itba.paw.persistence.projections;

import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetailRow {
    private final Integer itemId;
    private final Integer hostId;
    private final ItemStatusEnum status;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer capacity;
    private final Integer weight;
    private final Integer difficulty;
    private final Integer locationId;
    private final String locationName;
    private final String itemTypeName;
    private final Integer coverImageId;
    private final Double averageRating;
    private final Long totalReviews;
    private final Integer versionId;
}
