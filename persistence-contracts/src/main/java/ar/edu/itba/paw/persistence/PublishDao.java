package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;

public interface PublishDao {
    Version create(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            int weight,
            Integer difficulty,
            int locationOptionId,
            String timezone,
            String status,
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);
}
