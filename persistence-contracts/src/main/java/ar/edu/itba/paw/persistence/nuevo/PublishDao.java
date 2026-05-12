package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.PublishItem;
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
            java.math.BigDecimal maxWeightKg,
            Integer difficultyLevel,
            int locationOptionId,
            List<AvailabilityWindow> availabilities,
            List<ar.edu.itba.paw.models.nuevo.ImageUpload> images);

    Optional<PublishItem> findById(int itemId);

    List<AvailabilityWindow> listAvailabilities(int itemId);
}
