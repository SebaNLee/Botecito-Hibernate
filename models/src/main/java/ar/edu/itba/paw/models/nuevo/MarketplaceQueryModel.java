package ar.edu.itba.paw.models.nuevo;

import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketplaceQueryModel {
    private String searchQuery;
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer weight;
    private Integer difficulty;
    private Double minAvgRating;
    private String locationSlug;
    private String itemTypeSlug;
    private Integer page;
    private Integer pageSize;
    private String sortBy;
}
