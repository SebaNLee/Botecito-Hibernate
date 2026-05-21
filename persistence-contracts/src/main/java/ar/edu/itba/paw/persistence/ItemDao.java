package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Version;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId, int page, int pageSize);

    int countMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    Optional<Item> findItemByIdAndOwner(int itemId, int ownerId);

    boolean hasActiveOrFutureBookings(int itemId);

    void deleteItem(Item item);

    Optional<Version> findCurrentVersionByItemId(int itemId);

    boolean hasBookingReferencesByVersionId(int versionId);

    void persistVersion(Version version);

    void copyVersionContent(int sourceVersionId, Version targetVersion);

    Location getLocationReference(int locationId);

    Optional<Image> findImageWithDataById(int imageId);

    List<Integer> listImageIds(int itemId);

    Optional<Integer> uploadGalleryImage(int itemId, byte[] imageData);

    boolean deleteImageFromGallery(int imageId);

    boolean reorderGallery(int itemId, List<Integer> imageIdsInOrder);
}
