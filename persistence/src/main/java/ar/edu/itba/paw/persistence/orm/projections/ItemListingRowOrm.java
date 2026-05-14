package ar.edu.itba.paw.persistence.orm.projections;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.enums.ItemStatus;
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
    /** Version id for this row; set for all listing/detail queries that join {@code VersionOrm v}. */
    private final Integer versionId;

    public ItemModel toItemModel() {
        final ItemModel item = new ItemModel();
        item.setItemId(Objects.requireNonNull(itemId, "itemId"));
        item.setVersionId(Objects.requireNonNull(versionId, "versionId"));
        item.setHostId(hostId != null ? hostId : 0);
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

        if (weekday != null || startTime != null || endTime != null) {
            final AvailabilityWindow window = new AvailabilityWindow();
            if (weekday != null) {
                window.setWeekday(DayOfWeek.valueOf(weekday.name()));
            }
            window.setStartTime(startTime);
            window.setEndTime(endTime);
            item.setAvailabilityWindows(List.of(window));
        } else {
            item.setAvailabilityWindows(List.of());
        }

        return item;
    }
}
