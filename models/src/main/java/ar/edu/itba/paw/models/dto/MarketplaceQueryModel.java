package ar.edu.itba.paw.models.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketplaceQueryModel {
    private String searchQuery;
    private LocalDate requestedDate;
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
