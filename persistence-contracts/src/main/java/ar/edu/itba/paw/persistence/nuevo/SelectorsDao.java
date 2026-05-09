package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import java.util.List;

public interface SelectorsDao {
    List<Location> getLocationOptions();

    List<ItemTypeModel> getItemTypeOptions();
}
