package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;

public interface EditDao {

    boolean itemHasBookings(int itemId);

    Version edit(
            int itemId,
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

    Version overwrite(
            int versionId,
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
