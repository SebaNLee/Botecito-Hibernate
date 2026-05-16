package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import java.util.List;

public interface SelectorsDao {
    List<Location> getLocationOptions();

    List<ItemType> getItemTypeOptions();
}
