package ar.edu.itba.paw.models.nuevo;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketplaceSearchModel {
    private String searchQuery;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer weight;
    private Integer difficulty;
    private Double minAvgRating;
    private String location;
    private String itemType;
    private Integer page;
    private Integer pageSize;
    private String sortBy;
}
