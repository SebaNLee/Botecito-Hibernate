package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemCreateModel {
    private int ownerId;
    private int typeId;
    private String title;
    private String description;
    private int pricePerHour;
    private int capacityPeople;
    private BigDecimal maxWeightKg;
    private Integer difficultyLevel;
    private int locationOptionId;
    private String ownerDeleteToken;
}
