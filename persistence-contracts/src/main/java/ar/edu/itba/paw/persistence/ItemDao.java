package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import java.util.Optional;

public interface ItemDao {

    PageModel<Item> listOwnerItems(MyBoatsQueryModel query);

    Optional<Item> findItemById(int id);

    Optional<Image> findImageById(int id);

    void deleteItem(Item item);

    int getVersionCount(int itemId);

    void deleteVersion(Version version);
}
