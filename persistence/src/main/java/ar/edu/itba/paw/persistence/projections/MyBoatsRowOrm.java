package ar.edu.itba.paw.persistence.orm.projections;

import ar.edu.itba.paw.models.entity.ItemStatusEnumOrm;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyBoatsRowOrm {
    private final Integer itemId;
    private final Integer versionId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer difficulty;
    private final Integer locationId;
    private final Integer capacity;
    private final String locationName;
    private final ItemStatusEnumOrm status;
    private final Integer coverImageId;
    private final Boolean hasBlockingBookings;
    private final Boolean hasFutureBlockingBookings;
}
