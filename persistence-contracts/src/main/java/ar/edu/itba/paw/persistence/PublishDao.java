package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import java.util.Optional;

public interface PublishDao {
    int create(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            java.math.BigDecimal weight,
            Integer difficulty,
            int locationOptionId,
            String timezone,
            String status,
            List<AvailabilityWindow> availabilities,
            List<ar.edu.itba.paw.models.dto.ImageUpload> images);

    Optional<Version> findById(int itemId);

    List<Availability> listAvailabilities(int itemId);
}
