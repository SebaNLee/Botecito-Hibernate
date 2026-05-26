package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.services.SelectorsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectorsPresentation {

    private final SelectorsService selectorsInterface;

    public List<Location> getLocationOptions() {
        return selectorsInterface.getLocationOptions();
    }

    public List<ItemType> getItemTypeOptions() {
        return selectorsInterface.getItemTypeOptions();
    }

    public List<BookingStatusOptionModel> getBookingStatusOptions() {
        return selectorsInterface.getBookingStatusOptions();
    }
}
