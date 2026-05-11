package ar.edu.itba.paw.models.nuevo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemUpdateModel {
    private String title;
    private String description;
    private int pricePerHour;
    private Integer difficultyLevel;
    private int locationOptionId;
}
