package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import java.util.Optional;

public interface DetailDao {
    Optional<ItemModel> getItemById(int itemId);
}
