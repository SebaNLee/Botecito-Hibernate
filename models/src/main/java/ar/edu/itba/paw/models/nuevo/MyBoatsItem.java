package ar.edu.itba.paw.models.nuevo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyBoatsItem {
    private Integer id;
    private Integer versionId;
    private String title;
    private String description;
    private Integer pricePerHour;
    private Integer difficultyLevel;
    private Integer locationOptionId;
    private Integer capacityPeople;
    private String location;
    private Boolean active;
    private Integer coverImageId;
    private Boolean deleteDeactivates;
    private Boolean deleteDisabled;
}
