package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemForm {
    @NotNull
    @Min(1)
    private Integer typeId;

    @NotNull
    @Size(min = 1, max = 100)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Min(0)
    private Integer pricePerHour;

    @NotNull
    @Min(1)
    private Integer capacityPeople;

    @Min(0)
    private Double maxWeightKg;

    @Min(1)
    @Max(5)
    private Integer difficultyLevel;

    @NotNull
    @Size(min = 1, max = 120)
    private String location;
}
