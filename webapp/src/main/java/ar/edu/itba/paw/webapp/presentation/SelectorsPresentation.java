package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import ar.edu.itba.paw.services.nuevo.SelectorsInterface;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectorsPresentation {

    private final SelectorsInterface selectorsInterface;

    public List<Location> getLocationOptions() {
        return selectorsInterface.getLocationOptions();
    }

    public List<ItemTypeModel> getItemTypeOptions() {
        return selectorsInterface.getItemTypeOptions();
    }
}
