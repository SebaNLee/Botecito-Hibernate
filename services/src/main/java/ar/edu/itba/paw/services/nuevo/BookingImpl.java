package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingImpl implements BookingInterface {

    private final BookingDao bookingDao;

    @Override
    public PreBookingCreateResult createBooking(final PreBookingReq preBookingReq) {
        final int code = bookingDao.createBooking(preBookingReq);
        if (code > 0) {
            return new PreBookingCreateResult.Created(code);
        }
        if (code == BookingDao.RESULT_OUTSIDE_AVAILABILITY) {
            return PreBookingCreateResult.OutsideAvailability.INSTANCE;
        }
        if (code == BookingDao.RESULT_BOOKING_COLLISION) {
            return PreBookingCreateResult.Collision.INSTANCE;
        }
        return PreBookingCreateResult.Unexpected.INSTANCE;
    }

    @Override
    public List<Booking> getBookingsForVersion(final int versionId) {
        return bookingDao.getBookingsForVersion(versionId);
    }
}
