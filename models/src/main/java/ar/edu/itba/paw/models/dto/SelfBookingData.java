package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SelfBookingData {
    private final Item item;
    private final List<Booking> activeBookings;
    private final List<Booking> ownerSelfBlocks;
    private final List<LocalDate> offeredDates;
    private final List<LocalDate> blockedDates;
    private final LocalDate selectedDate;
    private final String timezone;
    private final DayTimelineData dayTimeline;
    private final LocalDate listingCalendarToday;
    private final LocalDate listingCalendarMaxInclusive;
    private final boolean hasTimelineAvailability;
}
