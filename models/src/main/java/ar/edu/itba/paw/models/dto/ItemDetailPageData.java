package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Item;
import java.time.LocalDate;
import lombok.Value;

@Value
public class ItemDetailPageData {
    Item item;
    AvailabilityData availabilityData;
    LocalDate listingCalendarToday;
    LocalDate listingCalendarMaxInclusive;
}
