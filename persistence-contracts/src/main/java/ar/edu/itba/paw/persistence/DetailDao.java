package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Item;
import java.util.Optional;

public interface DetailDao {

    Optional<Item> getItemDetail(int itemId, int reviewPage);
}
