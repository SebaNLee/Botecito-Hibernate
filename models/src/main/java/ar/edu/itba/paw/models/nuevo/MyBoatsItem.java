package ar.edu.itba.paw.models.nuevo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyBoatsItem {
    private Integer id;
    private String title;
    private Integer pricePerHour;
    private Integer capacityPeople;
    private String location;
    private Boolean active;
    private Integer coverImageId;
    private Boolean deleteDeactivates;
    private Boolean deleteDisabled;
    private String description;
    private Integer difficultyLevel;
    private Integer locationOptionId;
}
