package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublishItem {
    private final int id;
    private final int ownerId;
    private final int typeId;
    private final String title;
    private final String description;
    private final int pricePerHour;
    private final int capacityPeople;
    private final BigDecimal maxWeightKg;
    private final Integer difficultyLevel;
    private final String location;
    private final Integer coverImageId;
}
