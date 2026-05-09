package ar.edu.itba.paw.persistence.orm.projections;

import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.WeekdayEnumOrm;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketplaceSearchRowOrm {

    private final Integer itemId;
    private final Integer hostId;
    private final ItemStatusEnumOrm status;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer capacity;
    private final Integer weight;
    private final Integer difficulty;
    private final Integer locationId;
    private final String locationName;
    private final WeekdayEnumOrm weekday;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer coverImageId;
    private final Double averageRating;
    private final Long totalReviews;
}
