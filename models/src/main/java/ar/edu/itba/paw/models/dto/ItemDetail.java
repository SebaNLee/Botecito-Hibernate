package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Version;
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
        ItemStatusEnum status;
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
        List<Booking> bookings;
        List<Review> reviews;
        String versionTimezone;
        List<Availability> availabilityWindows;
        Version version;
    }
}
