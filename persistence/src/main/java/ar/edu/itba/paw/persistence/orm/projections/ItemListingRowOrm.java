package ar.edu.itba.paw.persistence.orm.projections;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.ItemStatus;
import ar.edu.itba.paw.persistence.orm.entities.ItemStatusEnumOrm;
import ar.edu.itba.paw.persistence.orm.entities.WeekdayEnumOrm;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** JPQL row for item listing queries (marketplace search, item detail). */
@Getter
@AllArgsConstructor
public class ItemListingRowOrm {

    private final Integer itemId;
    private final Integer hostId;
    private final ItemStatusEnumOrm status;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final Integer capacity;
    private final Integer weight;
    private final Integer difficulty;
    private final Integer locationId;
    private final String locationName;
    private final String itemTypeName;
    private final WeekdayEnumOrm weekday;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer coverImageId;
    private final Double averageRating;
    private final Long totalReviews;

    public ItemModel toItemModel() {
        final ItemModel item = new ItemModel();
        item.setId(Objects.requireNonNull(itemId, "itemId"));
        item.setHostId(hostId == null ? null : hostId.toString());
        item.setStatus(status == null ? null : ItemStatus.valueOf(status.name()));
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(price);
        item.setCapacity(Objects.requireNonNull(capacity, "capacity"));
        item.setWeight(Objects.requireNonNull(weight, "weight"));
        item.setDifficulty(Objects.requireNonNull(difficulty, "difficulty"));
        item.setLocationId(Objects.requireNonNull(locationId, "locationId"));
        item.setLocation(locationName);
        item.setItemTypeName(itemTypeName);

        if (coverImageId != null) {
            item.setImages(List.of("/image/" + coverImageId));
        } else {
            item.setImages(List.of("/css/boat-placeholder.svg"));
        }

        item.setAverageRating(averageRating == null ? 0D : averageRating);
        item.setTotalReviews(totalReviews == null ? 0 : totalReviews.intValue());

        item.setWeekday(weekday == null ? null : DayOfWeek.valueOf(weekday.name()));
        item.setStartTime(startTime);
        item.setEndTime(endTime);

        return item;
    }
}
