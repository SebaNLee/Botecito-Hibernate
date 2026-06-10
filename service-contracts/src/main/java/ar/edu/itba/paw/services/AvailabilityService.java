package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.DayTimelineData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public interface AvailabilityService {

    AvailabilityData buildAvailabilityData(
            List<Availability> availabilityWindows, List<Booking> bookings, String versionTimezone);

    DayTimelineData buildDayTimeline(
            LocalDate selectedDate,
            List<Availability> availabilities,
            List<Booking> bookings,
            List<Booking> ownerSelfBlocks,
            String timezone);

    boolean hasAvailabilityWindowsForDate(LocalDate selectedDate, List<Availability> availabilities);

    ZoneId listingZoneOrUtc(String versionTimezone);

    LocalDate listingCalendarToday(String versionTimezone);

    LocalDate listingCalendarMaxInclusive(String versionTimezone);
}
