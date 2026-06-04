package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import java.util.Optional;

public interface ManageItemDao {

    Optional<Item> findItemById(int itemId);

    int countVersionsByItemId(int itemId);

    Optional<Integer> findLatestVersionIdByItemId(int itemId);

    void deleteVersion(int versionId);
}
