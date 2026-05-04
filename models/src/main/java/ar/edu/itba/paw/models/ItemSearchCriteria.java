package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ItemSearchCriteria {
    private String searchQuery;
    private Integer locationOptionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private BigDecimal maxWeightKg;
    private Integer difficultyLevel;
    private Integer minAverageRating;
    private ItemSearchSort sort;
}
