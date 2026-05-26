package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemSearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import java.util.Optional;

public interface ItemService {

    ItemSearchResult listOwnerItems(int ownerId, String searchQuery, String status, String location,
                                    int page, int pageSize, String sortBy);

    Item findItemById(int id);

    Optional<Image> findImageById(int id);

    boolean userOwnsItem(Item item, int userId);

    boolean userOwnsItem(int itemId, int userId);

    Item requireOwnedItem(int itemId, int userId);

    // Rellena todos los datos que puede necesitar una version
    Version requireOwnedFullData(int itemId, int userId);
}
