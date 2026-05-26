package ar.edu.itba.paw.persistence.projections;

import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyBoatsRow {
    private final Integer itemId;
    private final Integer versionId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer difficulty;
    private final Integer locationId;
    private final Integer capacity;
    private final String locationName;
    private final ItemStatusEnum status;
    private final Integer coverImageId;
}
