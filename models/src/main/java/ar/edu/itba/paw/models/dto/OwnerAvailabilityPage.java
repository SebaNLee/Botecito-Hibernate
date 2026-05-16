package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.AvailabilityOrm;
import ar.edu.itba.paw.models.entity.BookingOrm;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class OwnerAvailabilityPage {
    private final MyBoatsItem item;
    private final List<AvailabilityOrm> availabilityWindows;
    private final List<BookingOrm> activeBookings;
    private final List<BookingOrm> ownerSelfBlocks;
    private final List<String> offeredDates;
    private final List<String> blockedDates;
    private final String selectedDate;
    private final String timezone;
}
