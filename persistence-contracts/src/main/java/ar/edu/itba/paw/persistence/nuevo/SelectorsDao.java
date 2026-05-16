package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import java.util.List;

public interface SelectorsDao {
    List<LocationOrm> getLocationOptions();

    List<ItemTypeOrm> getItemTypeOptions();
}
