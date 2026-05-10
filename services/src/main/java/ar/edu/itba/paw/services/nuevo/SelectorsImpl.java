package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import ar.edu.itba.paw.persistence.nuevo.SelectorsDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class SelectorsImpl implements SelectorsInterface {

    private final SelectorsDao selectorsDao;

    @Override
    public List<Location> getLocationOptions() {
        return selectorsDao.getLocationOptions();
    }

    @Override
    public List<ItemTypeModel> getItemTypeOptions() {
        return selectorsDao.getItemTypeOptions();
    }
}
