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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DisabledTimeSlotServiceImpl implements DisabledTimeSlotService {

    private final DisabledTimeSlotDao disabledTimeSlotDao;
    private final ItemDao itemDao;

    @Autowired
    public DisabledTimeSlotServiceImpl(final DisabledTimeSlotDao disabledTimeSlotDao, final ItemDao itemDao) {
        this.disabledTimeSlotDao = disabledTimeSlotDao;
        this.itemDao = itemDao;
    }

    @Override
    public DisabledTimeSlot disable(
            final int itemId, final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        validateRange(startTime, endTime);
        if (hasBlockingBooking(itemId, date, startTime, endTime)) {
            throw new SlotHasActiveBookingsException(
                    "Cannot disable slot with active bookings; cancel the bookings first");
        }
        return disabledTimeSlotDao.insert(itemId, date, startTime, endTime);
    }

    @Override
    public boolean reEnable(final int itemId, final int disabledSlotId) {
        return disabledTimeSlotDao.deleteById(itemId, disabledSlotId);
    }

    @Override
    public List<DisabledTimeSlot> listAll() {
        return disabledTimeSlotDao.listAll();
    }

    @Override
    public List<DisabledTimeSlot> listByItem(final int itemId) {
        return disabledTimeSlotDao.listByItem(itemId);
    }

    @Override
    public List<DisabledTimeSlot> listByItemBetween(
            final int itemId, final LocalDate fromDate, final LocalDate toDate) {
        return disabledTimeSlotDao.listByItemBetween(itemId, fromDate, toDate);
    }

    private boolean hasBlockingBooking(
            final int itemId, final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        for (final ItemBooking booking : itemDao.listBookingsByItemId(itemId)) {
            if (isBlockingBooking(booking) && overlaps(booking, date, startTime, endTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED;
    }

    private static boolean overlaps(
            final ItemBooking booking, final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        final OffsetDateTime bookingStart = booking.getStartTime();
        final OffsetDateTime bookingEnd = booking.getEndTime();
        if (bookingStart == null || bookingEnd == null) {
            return false;
        }
        final LocalDate bookingStartDate = bookingStart.toLocalDate();
        final LocalDate bookingEndDate = bookingEnd.toLocalDate();
        if (date.isBefore(bookingStartDate) || date.isAfter(bookingEndDate)) {
            return false;
        }
        final LocalTime effectiveStart = bookingStartDate.equals(date) ? bookingStart.toLocalTime() : LocalTime.MIN;
        final LocalTime effectiveEnd = bookingEndDate.equals(date) ? bookingEnd.toLocalTime() : LocalTime.MAX;
        return effectiveStart.isBefore(endTime) && startTime.isBefore(effectiveEnd);
    }

    private static void validateRange(final LocalTime startTime, final LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Disabled slot start must be before end");
        }
    }
}
