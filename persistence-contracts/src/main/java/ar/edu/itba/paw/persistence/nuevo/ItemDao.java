package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemCreateModel;
import ar.edu.itba.paw.models.nuevo.ItemUpdateModel;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    List<MyBoatsItem> listMyBoatsItemsByOwnerId(int ownerId);

    Optional<MyBoatsItem> findMyBoatsItemByIdForOwner(int itemId, int ownerId);

    Optional<MyBoatsItem> createMyBoatsItem(ItemCreateModel createModel);

    boolean updateMyBoatsItem(int itemId, int ownerId, ItemUpdateModel updateModel);

    boolean deleteMyBoatsItem(int itemId, int ownerId);

    int createPublicationVersion(int itemId, int ownerId, ItemUpdateModel update);

    boolean replaceVersionPrimaryImage(int versionId, byte[] imageData);

    boolean setItemActiveForOwner(int itemId, int ownerId, boolean active);
}
