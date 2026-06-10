package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AvailabilityImplTest {

    private final AvailabilityImpl availabilityService = new AvailabilityImpl();

    @Test
    public void buildAvailabilityDataMarksRefusedBookingAsAvailable() {
        Availability window = new Availability();
        window.setWeekday(WeekdayEnum.MONDAY);
        window.setStartTime(LocalTime.of(9, 0));
        window.setEndTime(LocalTime.of(12, 0));

        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        Booking refused = new Booking();
        refused.setStatus(BookingStatusEnum.REFUSED);
        refused.setStart(LocalDateTime.of(monday, LocalTime.of(9, 0)));
        refused.setEnd(LocalDateTime.of(monday, LocalTime.of(10, 0)));

        var data = availabilityService.buildAvailabilityData(List.of(window), List.of(refused), "UTC");

        assertFalse(data.getOfferedDates().isEmpty());
    }

    @Test
    public void buildDayTimelineExcludesSelfBlocksFromBookedRanges() {
        Availability window = new Availability();
        window.setWeekday(WeekdayEnum.MONDAY);
        window.setStartTime(LocalTime.of(9, 0));
        window.setEndTime(LocalTime.of(17, 0));

        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        Booking guestBooking = new Booking();
        guestBooking.setId(10);
        guestBooking.setStatus(BookingStatusEnum.PENDING);
        guestBooking.setStart(LocalDateTime.of(monday, LocalTime.of(10, 0)));
        guestBooking.setEnd(LocalDateTime.of(monday, LocalTime.of(11, 0)));

        Booking selfBlock = new Booking();
        selfBlock.setId(20);
        selfBlock.setStatus(BookingStatusEnum.CONFIRMED);
        selfBlock.setStart(LocalDateTime.of(monday, LocalTime.of(14, 0)));
        selfBlock.setEnd(LocalDateTime.of(monday, LocalTime.of(15, 0)));

        var timeline = availabilityService.buildDayTimeline(
                monday, List.of(window), List.of(guestBooking, selfBlock), List.of(selfBlock), "UTC");

        assertEquals(1, timeline.bookedRanges().size());
        assertEquals(1, timeline.selfBlocks().size());
    }
}
