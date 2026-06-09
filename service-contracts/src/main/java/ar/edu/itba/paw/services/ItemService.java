package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import java.util.Optional;

public interface ItemService {

    PageModel<Item> listOwnerItems(
            int ownerId, String searchQuery, String status, int page, int pageSize, String sortBy);

    Item findItemById(int id);

    Optional<Image> findImageById(int id);

    boolean userOwnsItem(Item item, int userId);

    boolean userOwnsItem(int itemId, int userId);

    Item requireOwnedItem(int itemId, int userId);

    // Rellena todos los datos que puede necesitar una version
    Version requireOwnedFullData(int itemId, int userId);

    int getVersionCount(int itemId);

    void deleteItem(Item item, boolean soft);

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
            List<AvailabilityWindow> availabilities,
            List<ImageUpload> images);

    void createNewVersion(
            Version current,
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

    void overwriteVersion(
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
