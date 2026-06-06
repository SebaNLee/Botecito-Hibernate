package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import java.util.List;

public interface PublishService {

    void create(
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            int weight,
            Integer difficulty,
            int locationOptionId,
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);

    /**
     * @return {@code true} if the publication was updated, {@code false} if nothing
     *         changed
     */
    boolean edit(
            int itemId,
            int ownerId,
            int typeId,
            String title,
            String description,
            int pricePerHour,
            int capacityPeople,
            int weight,
            Integer difficulty,
            int locationOptionId,
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);
}
