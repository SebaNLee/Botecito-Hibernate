package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    // Standard paged search query
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId, int page, int pageSize);

    // Will be returned by ^ in result DTO
    int countMyBoatsItemsByOwnerId(int ownerId);

    // Find by id. Control access in service
    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    // Standard find by id
    Optional<Image> findImageWithDataById(int imageId);

    // New ones:
    /*
    ItemSearchResult listOwnerItems(int ownerId, int page, int pageSize);

    Optional<Item> findItemById(int id);

    Optional<Image> findImageById(int id);

    boolean userOwnsItem(int itemId, int userId);
    */
}
