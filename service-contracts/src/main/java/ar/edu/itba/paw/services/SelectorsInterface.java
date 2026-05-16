package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import java.util.List;
import java.util.Map;

public interface SelectorsInterface {
    List<LocationOrm> getLocationOptions();

    List<ItemTypeOrm> getItemTypeOptions();

    List<BookingStatusOptionModel> getBookingStatusOptions();

    Map<String, String> getDifficultyOptions();
}
