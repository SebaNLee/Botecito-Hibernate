package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Item {
    private Integer id;
    private Integer ownerId;
    private Integer typeId;
    private String title;
    private String description;
    private Integer pricePerHour;
    private Integer capacityPeople;
    private BigDecimal maxWeightKg;
    private Integer difficultyLevel;
    private Integer locationOptionId;
    private String location;
    private Boolean active;
    private String ownerDeleteToken;
    private OffsetDateTime createdAt;
}
