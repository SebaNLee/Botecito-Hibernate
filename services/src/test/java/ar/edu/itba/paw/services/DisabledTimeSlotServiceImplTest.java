package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.DisabledTimeSlot;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.persistence.DisabledTimeSlotDao;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DisabledTimeSlotServiceImplTest {

    @InjectMocks
    private DisabledTimeSlotServiceImpl disabledTimeSlotService;

    @Mock
    private DisabledTimeSlotDao disabledTimeSlotDao;

    @Mock
    private ItemDao itemDao;

    @Test
    public void testDisableRejectsInvalidTimeRange() {
        final LocalDate date = LocalDate.of(2026, 5, 1);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> disabledTimeSlotService.disable(10, date, LocalTime.of(12, 0), LocalTime.of(12, 0)));
    }

    @Test
    public void testDisableRejectsSlotWhenThereIsOverlappingActiveBooking() {
        final LocalDate date = LocalDate.of(2026, 5, 1);
        final ItemBooking blockingBooking = booking(
                BookingState.BOOKING_CONFIRMED,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T12:00:00Z"));
        Mockito.when(itemDao.listBookingsByItemId(10)).thenReturn(List.of(blockingBooking));

        Assertions.assertThrows(
                SlotHasActiveBookingsException.class,
                () -> disabledTimeSlotService.disable(10, date, LocalTime.of(11, 0), LocalTime.of(13, 0)));
    }

    @Test
    public void testDisableCreatesSlotWhenOverlappingBookingIsNotBlocking() {
        final LocalDate date = LocalDate.of(2026, 5, 1);
        final ItemBooking nonBlockingBooking = booking(
                BookingState.BOOKING_CANCELLED,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T12:00:00Z"));
        Mockito.when(itemDao.listBookingsByItemId(10)).thenReturn(List.of(nonBlockingBooking));
        Mockito.when(disabledTimeSlotDao.insert(10, date, LocalTime.of(11, 0), LocalTime.of(13, 0)))
                .thenAnswer(invocation -> {
                    final DisabledTimeSlot created = new DisabledTimeSlot();
                    created.setItemId(invocation.getArgument(0));
                    created.setSlotDate(invocation.getArgument(1));
                    created.setStartTime(invocation.getArgument(2));
                    created.setEndTime(invocation.getArgument(3));
                    return created;
                });

        final DisabledTimeSlot result =
                disabledTimeSlotService.disable(10, date, LocalTime.of(11, 0), LocalTime.of(13, 0));

        Assertions.assertEquals(10, result.getItemId());
        Assertions.assertEquals(LocalTime.of(11, 0), result.getStartTime());
        Assertions.assertEquals(LocalTime.of(13, 0), result.getEndTime());
    }

    @Test
    public void testDisableCreatesSlotWhenBookingOverlapsDifferentDate() {
        final LocalDate date = LocalDate.of(2026, 5, 1);
        final ItemBooking activeBookingOtherDate = booking(
                BookingState.BOOKING_PAID,
                OffsetDateTime.parse("2026-05-02T10:00:00Z"),
                OffsetDateTime.parse("2026-05-02T12:00:00Z"));
        Mockito.when(itemDao.listBookingsByItemId(10)).thenReturn(List.of(activeBookingOtherDate));
        Mockito.when(disabledTimeSlotDao.insert(10, date, LocalTime.of(11, 0), LocalTime.of(13, 0)))
                .thenAnswer(invocation -> {
                    final DisabledTimeSlot created = new DisabledTimeSlot();
                    created.setSlotDate(invocation.getArgument(1));
                    return created;
                });

        final DisabledTimeSlot result =
                disabledTimeSlotService.disable(10, date, LocalTime.of(11, 0), LocalTime.of(13, 0));

        Assertions.assertEquals(date, result.getSlotDate());
    }

    private static ItemBooking booking(
            final BookingState state, final OffsetDateTime startTime, final OffsetDateTime endTime) {
        final ItemBooking booking = new ItemBooking();
        booking.setState(state);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        return booking;
    }
}
