package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
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

    /** Bookings where {@link OutcomingSearch#getGuestId()} is the guest (sent requests). */
    BookingSearchResult searchOutcomingBookings(OutcomingSearch search);

    /** Bookings on items where {@link IncomingSearch#getHostId()} is the host (received requests). */
    BookingSearchResult searchIncomingBookings(IncomingSearch search);
}
