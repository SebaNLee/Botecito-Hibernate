package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.PreBookingReq;
import java.util.List;

public interface BookingInterface {

    /**
     * Creates a pre-booking; interprets
     * {@link ar.edu.itba.paw.persistence.nuevo.BookingDao} return
     * codes into a {@link PreBookingCreateResult}.
     */
    PreBookingCreateResult createBooking(PreBookingReq preBookingReq);

    List<Booking> getBookingsForVersion(int versionId);
}
