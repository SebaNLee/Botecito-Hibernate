package ar.edu.itba.paw.models.nuevo;

import ar.edu.itba.paw.models.nuevo.enums.ItemStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemModel {
    private int itemId;
    private int versionId;
    private int hostId;

    private ItemStatus status;

    // Details
    private String title;
    private String description;
    private BigDecimal price;
    private int capacity;
    private int weight;
    private int difficulty;
    private int locationId;
    private String location;
    private String itemTypeName;

    // Media
    private List<String> images; // URLs or base64 encoded strings

    // Ratings
    private double averageRating;
    private int totalReviews;

    // Availability
    private List<AvailabilityWindow> availabilityWindows;
}
