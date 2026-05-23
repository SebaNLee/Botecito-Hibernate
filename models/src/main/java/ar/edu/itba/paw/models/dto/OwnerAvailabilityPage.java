package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class OwnerAvailabilityPage {
    private final Item item;
    private final List<Availability> availabilityWindows;
    private final List<Booking> activeBookings;
    private final List<Booking> ownerSelfBlocks;
    private final List<String> offeredDates;
    private final List<String> blockedDates;
    private final String selectedDate;
    private final String timezone;
}
