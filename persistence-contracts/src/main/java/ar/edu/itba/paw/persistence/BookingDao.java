package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.BookingQueryModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.PaymentProof;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public interface BookingDao {

    void insertBooking(final Booking booking);

    boolean canInsertBooking(final Booking booking, EnumSet<BookingStatusEnum> blockingStates);

    boolean canUpdate(final Booking booking, int excludeBookingId, EnumSet<BookingStatusEnum> blockingStates);

    boolean deleteBooking(final int id);

    Optional<Booking> findById(int bookingId);

    SearchResult<Booking> searchBookings(final BookingQueryModel query);

    void uploadPayment(PaymentProof paymentProof);

    void finalizeBookingsBefore(LocalDateTime minEndTime);

    void expireBookingsBefore(LocalDateTime minStartTime, EnumSet<BookingStatusEnum> cancellableStates);

    void deleteSelfBlocksBefore(LocalDateTime minEndTime);

    void deleteAllSelfBlocks(Item item);

    boolean itemHasBookings(Item item);

    List<Booking> getUpcomingBookings(Item item, LocalDateTime minDate, LocalDateTime maxDate);

    List<Booking> findBookingsToFinalizeBefore(LocalDateTime maxEndTime);

    List<Booking> findBookingsToExpireBefore(LocalDateTime minStartTime);
}
