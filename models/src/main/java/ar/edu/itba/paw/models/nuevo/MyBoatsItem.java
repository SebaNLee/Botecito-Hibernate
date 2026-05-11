package ar.edu.itba.paw.models.nuevo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
