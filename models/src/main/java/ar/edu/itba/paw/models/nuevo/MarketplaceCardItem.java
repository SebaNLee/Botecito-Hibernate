package ar.edu.itba.paw.models.nuevo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketplaceCardItem {
    int itemId;
    int hostId;
    int versionId;
    String title;
    String description;
    BigDecimal price;
    int capacity;
    int weight;
    int difficulty;
    int locationId;
    String location;
    String itemTypeName;
    double averageRating;
    int totalReviews;
    List<String> images;
}
