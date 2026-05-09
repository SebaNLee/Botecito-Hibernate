package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import java.util.Optional;

public interface DetailInterface {
    Optional<ItemModel> getItemById(int itemId);
}
