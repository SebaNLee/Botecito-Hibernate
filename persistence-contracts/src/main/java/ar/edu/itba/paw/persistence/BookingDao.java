package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

public interface BookingDao {

    void insertBooking(final Booking booking);

    boolean canInsertBooking(final Booking booking, EnumSet<BookingStatusEnum> blockingStates);

    boolean deleteBooking(final int id);

    Optional<Booking> findById(int bookingId);

    BookingSearchResult searchBookings(final BookingQueryModel query);

    void uploadPayment(PaymentProof paymentProof);

    void finalizeBookingsBefore(LocalDateTime maxEndTime);

    void expireBookingsBefore(LocalDateTime minStartTime, EnumSet<BookingStatusEnum> cancellableStates);
}
