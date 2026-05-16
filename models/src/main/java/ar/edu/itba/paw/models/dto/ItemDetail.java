package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.ItemStatusEnumOrm;
import ar.edu.itba.paw.models.entity.ReviewOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
public class ItemDetail {
    private List<VersionDetail> versions;

    @Value
    @Builder
    public static class VersionDetail {
        int itemId;
        int hostId;
        ItemStatusEnumOrm status;
        long versionId;
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
        List<BookingOrm> bookings;
        List<ReviewOrm> reviews;
        String versionTimezone;
        List<AvailabilityOrm> availabilityWindows;
        VersionOrm version;
    }
}
