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

    // TODO Eventualmente que sea create or update, reciben la misma info (parte de usar misma form ya esta hecho)
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

    /**
     * Empty when the publication does not exist; throws {@link
     * ar.edu.itba.paw.models.exceptions.ForbiddenOperationException} when it exists but the caller is not the host.
     */
    Optional<Version> findByIdForHost(int itemId, int callerId);

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
