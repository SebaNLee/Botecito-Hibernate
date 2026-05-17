package ar.edu.itba.paw.persistence.projections;

import ar.edu.itba.paw.models.dto.MarketplaceCardItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketplaceRow {
    private final Integer itemId;
    private final Integer hostId;
    private final Integer versionId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer capacity;
    private final Integer weight;
    private final Integer difficulty;
    private final Integer locationId;
    private final String locationName;
    private final String itemTypeName;
    private final Integer coverImageId;
    private final Double averageRating;
    private final Long totalReviews;

    public MarketplaceCardItem toCardItem() {
        final List<String> images =
                coverImageId != null ? List.of("/image/" + coverImageId) : List.of("/css/boat-placeholder.svg");
        return MarketplaceCardItem.builder()
                .itemId(Objects.requireNonNull(itemId, "itemId"))
                .hostId(hostId != null ? hostId : 0)
                .versionId(Objects.requireNonNull(versionId, "versionId"))
                .title(title)
                .description(description)
                .price(price)
                .capacity(Objects.requireNonNull(capacity, "capacity"))
                .weight(Objects.requireNonNull(weight, "weight"))
                .difficulty(Objects.requireNonNull(difficulty, "difficulty"))
                .locationId(Objects.requireNonNull(locationId, "locationId"))
                .location(locationName)
                .itemTypeName(itemTypeName)
                .averageRating(averageRating == null ? 0D : averageRating)
                .totalReviews(totalReviews == null ? 0 : totalReviews.intValue())
                .images(images)
                .build();
    }
}
