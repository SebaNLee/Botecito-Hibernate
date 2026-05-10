package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import java.util.List;

public interface SelectorsInterface {
    List<Location> getLocationOptions();

    List<ItemTypeModel> getItemTypeOptions();
}
