package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import java.util.List;
import java.util.Map;

public interface SelectorsService {
    List<Location> getLocationOptions();

    List<ItemType> getItemTypeOptions();

    List<BookingStatusOptionModel> getBookingStatusOptions();

    Map<String, String> getDifficultyOptions();
}
