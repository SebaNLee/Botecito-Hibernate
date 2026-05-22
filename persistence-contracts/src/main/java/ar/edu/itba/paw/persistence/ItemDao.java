package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
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

    Optional<Image> findImageWithDataById(int imageId);
}
