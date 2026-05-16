package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import java.util.List;
import java.util.Optional;

public interface ItemInterface {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId, int page, int pageSize);

    int countMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    boolean updateMyBoatsItem(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId);

    boolean deleteMyBoatsItem(int itemId, int ownerId);

    int createPublicationVersion(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId);

    // TODO temp fix for edit item page (should use PublishForm in the futuro)
    boolean replaceVersionPrimaryImage(int versionId, byte[] imageData);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    Optional<byte[]> findImageDataById(int imageId);

    List<Integer> listImageIds(int itemId);

    Optional<Integer> uploadGalleryImage(int itemId, int ownerId, byte[] imageData);

    boolean deleteImageFromGallery(int imageId);

    boolean reorderGallery(int itemId, int ownerId, List<Integer> imageIdsInOrder);
}
