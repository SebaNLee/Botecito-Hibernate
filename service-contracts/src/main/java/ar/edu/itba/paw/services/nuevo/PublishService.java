package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.VersionOrm;
import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ImageUpload;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PublishService {
    Optional<VersionOrm> create(int ownerId, int typeId, String title, String description,
            int pricePerHour, int capacityPeople, BigDecimal maxWeightKg,
            Integer difficultyLevel, int locationOptionId,
            List<AvailabilityWindow> availabilities, List<ImageUpload> images);

    Optional<VersionOrm> findById(int itemId);

    List<AvailabilityOrm> listAvailabilities(int itemId);

    Map<String, String> validate(String title, String description,
            int pricePerHour, int capacityPeople, BigDecimal maxWeightKg,
            Integer difficultyLevel, int locationOptionId,
            List<AvailabilityWindow> availabilities, List<ImageUpload> images);
}
