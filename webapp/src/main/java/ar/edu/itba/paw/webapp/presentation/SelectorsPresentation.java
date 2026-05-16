package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import ar.edu.itba.paw.models.nuevo.BookingStatusOptionModel;
import ar.edu.itba.paw.services.nuevo.SelectorsInterface;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectorsPresentation {

    private final SelectorsInterface selectorsInterface;

    public List<LocationOrm> getLocationOptions() {
        return selectorsInterface.getLocationOptions();
    }

    public List<ItemTypeOrm> getItemTypeOptions() {
        return selectorsInterface.getItemTypeOptions();
    }

    public List<BookingStatusOptionModel> getBookingStatusOptions() {
        return selectorsInterface.getBookingStatusOptions();
    }
}
