package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId, int page, int pageSize);

    int countMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    boolean deleteMyBoatsItem(int itemId, int ownerId);

    int createPublicationVersion(
            int itemId,
            int ownerId,
            String title,
            String description,
            int pricePerHour,
            Integer difficultyLevel,
            int locationOptionId);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    Optional<byte[]> findImageDataById(int imageId);

    List<Integer> listImageIds(int itemId);

    Optional<Integer> uploadGalleryImage(int itemId, byte[] imageData);

    boolean deleteImageFromGallery(int imageId);

    boolean reorderGallery(int itemId, List<Integer> imageIdsInOrder);
}
