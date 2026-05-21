package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
import java.util.List;
import java.util.Optional;

public interface ItemService {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId, int page, int pageSize);

    int countMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    MyBoatsItem requireOwnedItem(int itemId, int callerId);

    boolean deleteMyBoatsItem(int itemId, int ownerId);

    int createPublicationVersion(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficulty,
            int locationOptionId);

    void setItemActiveForOwner(int itemId, int ownerId, boolean active);

    Optional<Image> findImageWithDataById(int imageId);

    List<Integer> listImageIds(int itemId);

    Optional<Integer> uploadGalleryImage(int itemId, int ownerId, byte[] imageData);

    void deleteImageFromGallery(int itemId, int imageId, int callerId);

    boolean reorderGallery(int itemId, int ownerId, List<Integer> imageIdsInOrder);

    // TODO: Soft delete item method
}
