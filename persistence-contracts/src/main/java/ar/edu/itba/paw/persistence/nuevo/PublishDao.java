package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
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
            String timezone,
            String status,
            List<AvailabilityWindow> availabilities,
            List<ar.edu.itba.paw.models.nuevo.ImageUpload> images);

    Optional<VersionOrm> findById(int itemId);

    List<AvailabilityOrm> listAvailabilities(int itemId);
}
