package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemModel {
    private Integer id;
    private String hostId;
    private ItemStatus status;

    // Details
    private String title;
    private String description;
    private BigDecimal price;
    private Integer capacity;
    private Integer weight;
    private Integer difficulty;
    private Integer locationId;
    private String location;

    // Media
    private List<String> images; // URLs or base64 encoded strings

    // Ratings (ITEM-target reviews; same semantics as ReviewDao.ratingSummaryByTarget)
    private double averageRating;
    private int totalReviews;

    // Availability
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
}
