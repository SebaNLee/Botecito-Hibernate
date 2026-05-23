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

    Optional<Image> findImageWithDataById(int imageId);

    // New ones:
    /*
    ItemSearchResult listOwnerItems(int ownerId, int page, int pageSize)

    Optional<Item> findItemById(int id);

    Optional<Image> findImageById(int id);

    boolean userOwnsItem(int itemId, int userId);
    */
}
