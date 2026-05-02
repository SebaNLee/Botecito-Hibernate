package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ItemSearchCriteria {
    private String searchQuery;
    private Integer locationOptionId;
    private String date;
    private String startTime;
    private String endTime;
    private Integer capacity;
    private BigDecimal maxWeightKg;
    private Integer difficultyLevel;
    private Integer minAverageRating;
    private String sort;
}
