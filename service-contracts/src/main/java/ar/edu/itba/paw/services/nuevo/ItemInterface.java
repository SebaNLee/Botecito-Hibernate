package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import java.util.List;
import java.util.Optional;

public interface ItemInterface {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    // Eventualmente que sea create or update, reciben la misma info
    Optional<MyBoatsItem> createMyBoatsItem(ItemCreateModel createModel);

    boolean updateMyBoatsItem(int itemId, int ownerId, ItemUpdateModel updateModel);

    boolean deleteMyBoatsItem(int itemId, int ownerId);

    int createPublicationVersion(int itemId, int ownerId, ItemUpdateModel update);

    // TODO temp fix for edit item page (should use PublishForm in the futuro)
    boolean replaceVersionPrimaryImage(int versionId, byte[] imageData);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);

    // TODO: Soft delete item method
}
