package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import java.util.List;

public interface SelectorsDao {
    List<LocationOrm> getLocationOptions();

    List<ItemTypeOrm> getItemTypeOptions();
}
