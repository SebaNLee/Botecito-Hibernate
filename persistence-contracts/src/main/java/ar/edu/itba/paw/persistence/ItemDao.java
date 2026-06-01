package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import java.util.Optional;

public interface ItemDao {

    SearchResult<ar.edu.itba.paw.models.entity.Item> listOwnerItems(MyBoatsQueryModel query);

    Optional<Item> findItemById(int id);

    Optional<Image> findImageById(int id);
}
