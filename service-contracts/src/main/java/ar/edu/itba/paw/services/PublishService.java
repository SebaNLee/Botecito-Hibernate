package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Version;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PublishService {
    Optional<Version> create(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            BigDecimal maxWeightKg,
            Integer difficultyLevel,
            int locationOptionId,
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);

    Optional<Version> findById(int itemId);

    List<Availability> listAvailabilities(int itemId);

    Map<String, String> validate(
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            BigDecimal maxWeightKg,
            Integer difficultyLevel,
            int locationOptionId,
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);
}
